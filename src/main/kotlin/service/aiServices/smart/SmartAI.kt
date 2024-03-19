package service.aiServices.smart

import entity.AquaGhetto
import entity.Player
import entity.aIActions.*
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.tileTypes.CoinTile
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.RootService
import service.aiServices.smart.evaluateActions.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * Main class for smartAI, consist of important classes and functions like
 * evaluate classes, makeTurn and minimax algorithm function.
 *
 * @param rootService instance of the [RootService] for access to other services
 * @param player refers to the AI that uses this class to determine its moves.
 */
class SmartAI(val rootService: RootService, val player: Player) {

    private val evaluateActionFreePrisoner = EvaluateFreePrisonerService(this)
    private val evaluateActionTakeBus = EvaluateTakeBusService(this)
    private val evaluateAddTileToBus = EvaluateAddTileToPrisonBusService(this)
    private val evaluateBuyPrisoner = EvaluateBuyPrisonerService(this)
    private val evaluateExpandPrison = EvaluateExpandPrisonGridService(this)
    private val evaluateMoveEmployee = EvaluateMoveEmployeeService(this)
    private val evaluateMoveOwnPrisoner = EvaluateMoveOwnPrisonerService(this)
    val evaluateGamePosition = EvaluateGamePositionService(this)
    val evaluateBestPosition = EvaluateBestPosition(this)

    private val checkLayers = 4

    init {
        require(player.type == PlayerType.AI) {"Player is not an AI"}
    }

    /**
     * Main function that's called. When AI makes a turn.
     */
    fun makeTurn(game: AquaGhetto) {
        val startTime = System.currentTimeMillis()

        val action = this.minMax(game.clone(), checkLayers)

        val endTime = System.currentTimeMillis()
        println("Time: ${endTime - startTime}")

        if (!action.validAction) {
            println("Found no valid action?")
        } else {
            this.executeAction(game, action)
        }
        println(count)
    }

    /**
     * This is the function called by makeTurn when AI determines the best action.
     * Here the action realized.
     */
    private fun executeAction(game: AquaGhetto, aiAction: AIAction) {
        when (aiAction) {
            is ActionAddTileToBus -> {
                val card = rootService.playerActionService.drawCard()
                val busIndex = when (card) {
                    is CoinTile -> aiAction.indexBusCoin
                    is PrisonerTile -> aiAction.indexBusPrisoner[card.prisonerType] ?: -1
                    else -> -1
                }
                if (busIndex == -1) throw IllegalStateException("Found invalid tile in drawStack")
                println("execute action place tile ${card.javaClass.name} on $busIndex")
                rootService.playerActionService.addTileToPrisonBus(card, game.prisonBuses[busIndex])
            }
            is ActionMovePrisoner -> {
                val placeCard = aiAction.placeCard
                val prisoner = placeCard.placePrisoner
                val bonus = rootService.playerActionService.movePrisonerToPrisonYard(prisoner.first, prisoner.second)
                this.placeCardBonus(aiAction.placeCard, bonus)
                println("execute action move prisoner from own isolation to ${prisoner.first}  ${prisoner.second}")
            }
            is ActionMoveEmployee -> {
                val source = aiAction.source
                val destination = aiAction.destination
                rootService.playerActionService.moveEmployee(source.first, source.second,
                    destination.first, destination.first)
                println("execute action move employee from ${source.first}  ${source.second} to ${source.first}  ${source.second}")
            }
            is ActionBuyPrisoner -> {
                val boughtFrom = aiAction.buyFrom
                val placeCard = aiAction.placeCard
                val prisoner = placeCard.placePrisoner
                val bonus = rootService.playerActionService.buyPrisonerFromOtherIsolation(boughtFrom,
                    prisoner.first, prisoner.second)
                this.placeCardBonus(aiAction.placeCard, bonus)
                println("execute action buy prisoner from ${boughtFrom.name} and place ${prisoner.first}  ${prisoner.second}")
            }
            is ActionFreePrisoner -> {
                rootService.playerActionService.freePrisoner()
                println("execute action free prisoner")
            }
            is ActionExpandPrison -> {
                val isBig = aiAction.isBig
                val location = aiAction.location
                val rotation = aiAction.rotation
                rootService.playerActionService.expandPrisonGrid(isBig, location.first, location.second, rotation)
                println("execute action expand prison $isBig $location $rotation")
            }
            is ActionTakeBus -> {
                takeBus(aiAction, game)
                print("execute action take bus:")
                for (place in aiAction.placeCards) {
                    print("normalPrisoner: ${place.placePrisoner} firstBonusEmployee: ${place.firstTileBonusEmployee} " +
                            "bonusPrisoner: ${place.placeBonusPrisoner} secondBonusEmployee:${place.secondTileBonusEmployee}   ")
                }
                println()
                /*after all tiles were placed determineNextPlayer needs to be called*/
                rootService.gameService.determineNextPlayer(true)
            }
            else -> {
                println("End no action")
            }
        }
    }

    /**
     * Function executing the ActionTakeBus
     * Takes a bus depending on the information in [aiAction] and places the cards, bonuses on the
     * locations defined in [aiAction]
     *
     * @param aiAction information about performing the action
     * @param game the current game
     */
    private fun takeBus(aiAction: ActionTakeBus, game: AquaGhetto) {
        val takenBus = game.prisonBuses[aiAction.bus]
        rootService.playerActionService.takePrisonBus(takenBus) /*remove coins from bus*/
        val tiles = takenBus.tiles.filterNotNull().toMutableList()
        if (aiAction.placeCards.size != tiles.size) {
            //TODO add emergency action?
            println("Error AI action did not matched bus tiles")
            return
        }
        for (i in tiles.indices) {
            val tile = tiles[i]

            /*remove tile from bus*/
            takenBus.tiles[i] = null

            if (tile !is PrisonerTile) {
                println("Found non prisoner tile in bus, this should not happen")
                continue
            }

            val placeCard = aiAction.placeCards[i]
            val prisoner = placeCard.placePrisoner

            println("place card ${tile.id} at (${placeCard.placePrisoner.first}, ${placeCard.placePrisoner.second})")

            val bonus = rootService.playerActionService.placePrisoner(tile, prisoner.first, prisoner.second)
            this.placeCardBonus(placeCard, bonus)
        }
    }

    private var count = 0

    /**
     * Minimax algorithm called by makeTurn to determine the best move for the AI.
     * This function goes through each possible moves and finally picks the one that leads the
     * AI to the best score that it can get.
     * Unlike the usual AI, we did not use maximize parameter since each Player tries to maximize
     * their own score. That's why each node in minMax here represents the AI moves.
     *
     * @param game the game of AquaGhetto
     * @param depth depth of the binary tree used in MinMax. The greater the depth,
     * the more find a good action, but more memory we use.
     * @return returns the best AIAction by using getBestAction function.
     */
    fun minMax(game: AquaGhetto, depth: Int): AIAction {
        count++

        if (depth == 0 || checkGameEnd(game)) {
            return AIAction(false, evaluateGamePosition.evaluateCurrentPosition(game, player))
        }

        val undoData = this.simulateSetUpNewRound(game)

        /*runs the 7 actions parallel*/
        /*
        if (depth == checkLayers - 0 || depth == checkLayers - 1) {
            return minMaxNewThread(game, depth)
        }
        */

        val scoreAddTileToPrisonBus = evaluateAddTileToBus.getScoreAddTileToPrisonBus(game, depth - 1)
        val scoreMoveOwnPrisoner = evaluateMoveOwnPrisoner.getScoreMoveOwnPrisoner(game, depth - 1)
        val scoreMoveEmployee = evaluateMoveEmployee.getScoreMoveEmployee(game, depth - 1, player)
        val scoreBuyPrisoner = evaluateBuyPrisoner.getScoreBuyPrisoner(game, depth - 1)
        val scoreFreePrisoner = evaluateActionFreePrisoner.freePrisoner(game, depth - 1)
        val scoreExpandPrison = evaluateExpandPrison.getScoreExpandPrisonGrid(game, depth - 1)
        val scoreTakeBus = evaluateActionTakeBus.takeBus(game, depth - 1)

        val scoreList = mutableListOf(scoreAddTileToPrisonBus, scoreMoveOwnPrisoner, scoreMoveEmployee,
            scoreBuyPrisoner, scoreFreePrisoner, scoreExpandPrison, scoreTakeBus)

        simulateUndoNewRound(game, undoData)

        val bestAction = this.getBestAction(scoreList)

        return bestAction
    }

    //TODO check if this is useful / working
    //TODO clone game
    /**
     * Threaded version of minMax algorithm.
     */
    private fun minMaxNewThread(game: AquaGhetto, depth: Int): AIAction {
        val threads = mutableListOf<Thread>()

        val undoData = this.simulateSetUpNewRound(game)

        var scoreAddTileToPrisonBus: AIAction? = null
        var scoreMoveOwnPrisoner: AIAction? = null
        var scoreMoveEmployee: AIAction? = null
        var scoreBuyPrisoner: AIAction? = null
        var scoreFreePrisoner: AIAction? = null
        var scoreExpandPrison: AIAction? = null
        var scoreTakeBus: AIAction? = null

        //val thr = Thread.currentThread()
        val lock = ReentrantLock()
        val condition = lock.newCondition()


        threads.add(thread {
            scoreAddTileToPrisonBus = evaluateAddTileToBus.getScoreAddTileToPrisonBus(game.clone(), depth - 1)
            lock.withLock { condition.signal() }
        })
        threads.add(thread {
            scoreMoveOwnPrisoner = evaluateMoveOwnPrisoner.getScoreMoveOwnPrisoner(game.clone(), depth - 1)
            lock.withLock { condition.signal() }
        })
        threads.add(thread {
            scoreMoveEmployee = evaluateMoveEmployee.getScoreMoveEmployee(game.clone(), depth - 1, player)
            lock.withLock { condition.signal() }
        })
        threads.add(thread {
            scoreBuyPrisoner = evaluateBuyPrisoner.getScoreBuyPrisoner(game.clone(), depth - 1)
            lock.withLock { condition.signal() }
        })
        threads.add(thread {
            scoreFreePrisoner = evaluateActionFreePrisoner.freePrisoner(game.clone(), depth - 1)
            lock.withLock { condition.signal() }
        })
        threads.add(thread {
            scoreExpandPrison = evaluateExpandPrison.getScoreExpandPrisonGrid(game.clone(), depth - 1)
            lock.withLock { condition.signal() }
        })
        threads.add(thread {
            scoreTakeBus = evaluateActionTakeBus.takeBus(game.clone(), depth - 1)
            lock.withLock { condition.signal() }
        })

        lock.withLock {
            val startTime = System.currentTimeMillis()

            while ((startTime + 8 * 1000) > System.currentTimeMillis()) {
                val timeWait = 8000 - (System.currentTimeMillis() - startTime)
                println(timeWait)
                condition.await(timeWait, TimeUnit.MILLISECONDS)

                if (scoreAddTileToPrisonBus != null &&
                    scoreMoveOwnPrisoner != null &&
                    scoreMoveEmployee != null &&
                    scoreBuyPrisoner != null &&
                    scoreFreePrisoner != null &&
                    scoreExpandPrison != null &&
                    scoreTakeBus != null) {
                    break
                }
            }

            if (depth == checkLayers) condition.await(1000, TimeUnit.MILLISECONDS)
        }

        /*
        for (t in threads) {
            t.interrupt()
        }
        */

        /*
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            /*do nothing*/
        }
        */

        val scoreListNulls = mutableListOf(scoreAddTileToPrisonBus, scoreMoveOwnPrisoner, scoreMoveEmployee,
            scoreBuyPrisoner, scoreFreePrisoner, scoreExpandPrison, scoreTakeBus)

        simulateUndoNewRound(game, undoData)

        return this.getBestAction(scoreListNulls.filterNotNull())
    }

    /**
     * Function to get the best action
     *
     * @param actionList a list of possible actions the AI/player could make
     * @return the action this AI/player should/would perform
     */
    private fun getBestAction(actionList: List<AIAction>): AIAction {
        var bestScore = Integer.MIN_VALUE
        var bestAction: AIAction? = null
        for (element in actionList) {
            if (element.score > bestScore && element.validAction) {
                bestAction = element
                bestScore = element.score
            }
        }
        return bestAction ?: AIAction(false,0)
    }

    /**
     * Checks if a game has ended, by checking if all players have taken a bus and the final stack does not
     * contain exactly 15 cards
     *
     * @param game the game of AquaGhetto
     * @return true if the game has ended, otherwise false
     */
    private fun checkGameEnd(game: AquaGhetto): Boolean {
        /*check if all players have taken a bus*/
        for (p in game.players) {
            if (p.takenBus == null) return false
        }

        /*if all players have taken a bus and the final stack does not contain 15 cards the game has ended*/
        return game.finalStack.size != 15
    }

    /**
     * Function to place a card bonus depending on the info provided in [placeCard] and [bonus]
     *
     * @param placeCard info about the location, where to place a tile
     * @param bonus the bonus obtained by placing a card
     */
    private fun placeCardBonus(placeCard: PlaceCard , bonus: Pair<Boolean,PrisonerTile?>) {
        /*place possible employee if valid*/
        placeEmployee(bonus.first, placeCard.firstTileBonusEmployee)

        /*place possible baby*/
        var secondBonus: Pair<Boolean, PrisonerTile?>? = null
        val bonusBaby = bonus.second
        val bonusLocation = placeCard.placeBonusPrisoner
        if (bonusBaby != null && bonusLocation == null) {
            /*If there is no place in prison area to place the bonus baby,
            * then bonus card goes to isolation.*/
            rootService.playerActionService.placePrisoner(bonusBaby, -101,-101)
            println("Error AI action did not matched bonus")
        } else if (bonusBaby == null && bonusLocation != null) {
            /*do nothing I guess*/
            println("Error AI action did not matched bonus")
        } else if (bonusBaby != null && bonusLocation != null) {
            secondBonus = rootService.playerActionService.placePrisoner(bonusBaby,
                bonusLocation.first, bonusLocation.second)
        }

        /*Second bonus can only "contain" a new employee*/
        if (secondBonus == null) return
        placeEmployee(secondBonus.first, placeCard.secondTileBonusEmployee)
    }

    /**
     * Places a prisoner at the location if provided and if bonus is true
     */
    private fun placeEmployee(bonus: Boolean, employee: Pair<Int,Int>?) {
        if (bonus && (employee == null)) {
            //TODO add emergency action?
            println("Error AI action did not matched bonus")
        } else if (!bonus && (employee != null)) {
            /*do nothing I guess*/
            println("Error AI action did not matched bonus")
        } else if (bonus && employee != null) {
            rootService.playerActionService.moveEmployee(-101,-101,
                employee.first, employee.second)
        }
    }

    /**
     * set up the current and the next player.
     * @param game the game of AquaGhetto
     * @param busWasTakenInThisRound true if bus has just taken by current Player,
     * used in order to adjust current and next player.
     */
    fun getNextAndOldPlayer(game: AquaGhetto,busWasTakenInThisRound: Boolean): Pair<Int,Int> {
        val oldPlayer = game.currentPlayer

        val isTwoPlayerGame = game.players.size == 2

        val numberOfBussesLeft = if (isTwoPlayerGame) {
            game.players.count { it.takenBus == null }
        } else {
            game.prisonBuses.size
        }

        when (numberOfBussesLeft) {
            1 ->  {
                // Without that, it would still be the second-to-last player's turn
                if (busWasTakenInThisRound) {
                    do {
                        game.currentPlayer = (game.currentPlayer + 1) % game.players.size
                    } while (game.players[game.currentPlayer].takenBus != null)
                }
            }
            0 -> {
                /*all players have taken a bus*/
                /*moving the bus in the middle and back should get handled in EvaluateTakeBusService*/
                /*end game should get handled in the minMax function*/
            }
            else -> {
                /*
                sets the current player to the next player
                provided that they didn't take a bus
                */
                do {
                    game.currentPlayer = (game.currentPlayer + 1) % game.players.size
                } while (game.players[game.currentPlayer].takenBus != null)
            }
        }

        val nextPlayer = game.currentPlayer

        return Pair(oldPlayer, nextPlayer)
    }

    /**
     * Is used to set up the new round. Used in minMax function.
     * basically sets up the player's buses and buses on the mid.
     */
    private fun simulateSetUpNewRound(game: AquaGhetto): MutableList<MutableList<Pair<Tile?,Boolean>>>?  {
        /*moving the buses back in the middle is handled in EvaluateTakeBusService*/
        var takenBuses = 0
        for (p in game.players) {
            if (p.takenBus != null) takenBuses++
        }
        //println("$takenBuses   ${game.players.size}")
        if (takenBuses != game.players.size) {
            return null /*round has not ended*/
        }

        val busData = mutableListOf<MutableList<Pair<Tile?,Boolean>>>()

        for (bus in game.prisonBuses) {
            val busList = mutableListOf<Pair<Tile?,Boolean>>()
            for (i in bus.tiles.indices) {
                busList.add(Pair(bus.tiles[i], bus.blockedSlots[i]))
                bus.tiles[i] = null
            }
            busData.add(busList)
        }
        return busData
    }
    /**
     *Used to undo the new round. Used in the minMax function.
     *resets the busses to what they were before the evaluate functions were called.
     */
    private fun simulateUndoNewRound(game: AquaGhetto, busData: MutableList<MutableList<Pair<Tile?,Boolean>>>?) {
        if (busData == null) return

        for (i in game.prisonBuses.indices) {
            val bus = game.prisonBuses[i]
            val data = busData[i]

            for (t in data.indices) {
                bus.tiles[t] = data[t].first
                bus.blockedSlots[t] = data[t].second
            }
        }
    }

    /**
     * Simulate the prisoner or employee placements in order to see the best possible scenario
     * for the AI Player. Used in evaluation classes.
     */
    fun simulatePlacement(placeCard: PlaceCard, tile: PrisonerTile, coin: Boolean, player: Player): Pair<PrisonerTile, PrisonerTile>?{
        val board = player.board
        val returnValue: Pair<PrisonerTile, PrisonerTile>? = null

        val prisoner = placeCard.placePrisoner
        board.setPrisonYard(prisoner.first, prisoner.second, tile)

        val bonusFirstEmployee = placeCard.firstTileBonusEmployee
        if (bonusFirstEmployee != null) {
            val x = bonusFirstEmployee.first
            val y = bonusFirstEmployee.second

            when (x) {
                -102 -> {
                    player.hasJanitor = true
                }
                -103 -> {
                    player.secretaryCount++
                }
                -104 -> {
                    player.lawyerCount++
                } else -> {
                    board.setPrisonYard(x, y, GuardTile())
                }
            }
        }

        if (placeCard.placeBonusPrisoner != null) {
            val baby = evaluateBestPosition.checkBabyNotRemove(player)
            if (baby != null) {
                val babyTile = PrisonerTile(-1, PrisonerTrait.BABY, baby.first.prisonerType)
                board.setPrisonYard(prisoner.first, prisoner.second, babyTile)
                baby.first.breedable = false
                baby.second.breedable = false
            } else {
                println("This should not happen. Baby pos but no baby?")
            }

        }

        val bonusSecondEmployee = placeCard.firstTileBonusEmployee
        if (bonusSecondEmployee != null) {
            val x = bonusSecondEmployee.first
            val y = bonusSecondEmployee.second

            when (x) {
                -102 -> {
                    player.hasJanitor = true
                }
                -103 -> {
                    player.secretaryCount++
                }
                -104 -> {
                    player.lawyerCount++
                } else -> {
                board.setPrisonYard(x, y, GuardTile())
                }
            }
        }

        if (coin) player.coins++

        /*return parent tiles to allow undo*/
        return returnValue
    }
    /**
     * Undo everything done in simulate placement function. in order to arrange recursion calls.
     * Used in evaluation classes.
     */
    fun undoSimulatePlacement(placeCard: PlaceCard, coin: Boolean, player: Player, parentTiles: Pair<PrisonerTile, PrisonerTile>?) {
        val board = player.board

        val prisoner = placeCard.placePrisoner
        board.setPrisonYard(prisoner.first, prisoner.second, null)

        val bonusFirstEmployee = placeCard.firstTileBonusEmployee
        if (bonusFirstEmployee != null) {
            val x = bonusFirstEmployee.first
            val y = bonusFirstEmployee.second

            when (x) {
                -102 -> {
                    player.hasJanitor = false
                }
                -103 -> {
                    player.secretaryCount--
                }
                -104 -> {
                    player.lawyerCount--
                } else -> {
                board.setPrisonYard(x, y, null)
            }
            }
        }

        if (placeCard.placeBonusPrisoner != null && parentTiles != null) {
            board.setPrisonYard(prisoner.first, prisoner.second, null)
            parentTiles.first.breedable = true
            parentTiles.second.breedable = true
        }

        val bonusSecondEmployee = placeCard.firstTileBonusEmployee
        if (bonusSecondEmployee != null) {
            val x = bonusSecondEmployee.first
            val y = bonusSecondEmployee.second

            when (x) {
                -102 -> {
                    player.hasJanitor = false
                }
                -103 -> {
                    player.secretaryCount--
                }
                -104 -> {
                    player.lawyerCount--
                } else -> {
                board.setPrisonYard(x, y, null)
                }
            }
        }

        if (coin) player.coins++
    }


}


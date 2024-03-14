package service.aiServices.smart

import entity.AquaGhetto
import entity.Player
import entity.aIActions.*
import entity.enums.PlayerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import service.RootService
import service.aiServices.smart.evaluateActions.*
import kotlin.concurrent.thread

class SmartAI(val rootService: RootService, val player: Player) {

    private val evaluateActionFreePrisoner = EvaluateFreePrisonerService(this)
    private val evaluateActionTakeBus = EvaluateTakeBusService(this)
    private val evaluateAddTileToBus = EvaluateAddTileToPrisonBusService(this)
    private val evaluateBuyPrisoner = EvaluateBuyPrisonerService(this)
    private val evaluateExpandPrison = EvaluateExpandPrisonGridService(this)
    private val evaluateMoveEmployee = EvaluateMoveEmployeeService(this)
    private val evaluateMoveOwnPrisoner = EvaluateMoveOwnPrisonerService(this)
    private val evaluateGamePosition = EvaluateGamePositionService(this)

    private val checkLayers = 5

    init {
        require(player.type == PlayerType.AI) {"Player is not an AI"}
    }

    /*wird von ai service aufgerufen*/
    /*ruft dann intern minMax auf und so*/
    /*mal schauen ob wir hier multithreading einbauen, mehr als ein rechner zur berechnung?*/
    fun makeTurn(game: AquaGhetto) {
        val action = this.minMax(game, checkLayers, 0, 0)
        if (!action.validAction) {
            println("Found no valid action?")
        }
        this.executeAction(game, action)
    }

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
                            "bonusPrisoner: ${place.placeBonusPrisoner} secondBonusEmployee:${place.secondTileBonusEmployee}")
                }
                println()
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
        val tiles = takenBus.tiles.filterNotNull()
        if (aiAction.placeCards.size != tiles.size) {
            //TODO add emergency action?
            println("Error AI action did not matched bus tiles")
            return
        }
        for (i in tiles.indices) {
            val tile = tiles[i]
            if (tile !is PrisonerTile) {
                println("Found non prisoner tile in bus, this should not happen")
                continue
            }
            val placeCard = aiAction.placeCards[i]
            val prisoner = placeCard.placePrisoner
            val bonus = rootService.playerActionService.placePrisoner(tile, prisoner.first, prisoner.second)
            this.placeCardBonus(placeCard, bonus)
        }
    }

    /*
     * actionsInspected ist nur dafür da um zu verfolgen wie viele wege schon bewertet wurden
     * damit kann einfacher gesagt werden welche optimierungen weniger wege betrachten
     * kann später sonst auch gelöscht werden
     */
    fun minMax(game: AquaGhetto, depth: Int, maximize: Int, actionsChecked: Int): AIAction {

        //println("call min max function $actionsChecked")

        if (depth == 0 || checkGameEnd(game)) { /*hier überprüfung ob maximale tiefe erreicht wurde oder spiel schon geendet hat*/
            return AIAction(false, evaluateGamePosition.evaluateCurrentPosition())
        }

        /*runs the 7 actions parallel*/
        /*
        if (depth == checkLayers - 1) {
            return minMaxNewThread(game, depth, maximize, actionsChecked)
        }
        */

        /*die verschiedenen züge die ein spieler machen kann, wenn ein zug nicht möglich ist, hat dieser den schlechtesten wert
        * abhängig von maximize + oder - unendlich*/
        /*funktion gibt den score des standes nach der aktion zurück und was gemacht werden muss damit man zu diesem
        * score kommt, damit dies nicht ernuet berechnet werden muss*/
        /*erstelle objekte sollten sich (hoffentlich) in grenzen halten*/
        val scoreAddTileToPrisonBus = evaluateAddTileToBus.getScoreAddTileToPrisonBus(game, depth - 1, maximize, actionsChecked)
        val scoreMoveOwnPrisoner = evaluateMoveOwnPrisoner.getScoreMoveOwnPrisoner(game, depth - 1, maximize, actionsChecked)
        val scoreMoveEmployee = evaluateMoveEmployee.getScoreMoveEmployee(game, depth - 1, maximize, actionsChecked)
        val scoreBuyPrisoner = evaluateBuyPrisoner.getScoreBuyPrisoner(game, depth - 1, maximize, actionsChecked)
        val scoreFreePrisoner = evaluateActionFreePrisoner.freePrisoner(game, depth - 1, maximize, actionsChecked)
        val scoreExpandPrison = evaluateExpandPrison.getScoreExpandPrisonGrid(game, depth - 1, maximize, actionsChecked)
        val scoreTakeBus = evaluateActionTakeBus.takeBus(game, depth - 1, maximize, actionsChecked)

        val scoreList = mutableListOf(scoreAddTileToPrisonBus, scoreMoveOwnPrisoner, scoreMoveEmployee,
            scoreBuyPrisoner, scoreFreePrisoner, scoreExpandPrison, scoreTakeBus)

        /*
        for (action in scoreList) {
            if (!action.validAction) continue
            println("${action.score}   ${action.validAction}   ${action.javaClass.name}")
        }
        */


        return this.getBestAction(maximize, scoreList, game)
    }

    //TODO check if this is useful / working
    //TODO clone game
    private fun minMaxNewThread(game: AquaGhetto, depth: Int, maximize: Int, actionsChecked: Int): AIAction {
        val threads = mutableListOf<Thread>()

        var scoreAddTileToPrisonBus: AIAction? = null
        var scoreMoveOwnPrisoner: AIAction? = null
        var scoreMoveEmployee: AIAction? = null
        var scoreBuyPrisoner: AIAction? = null
        var scoreFreePrisoner: AIAction? = null
        var scoreExpandPrison: AIAction? = null
        var scoreTakeBus: AIAction? = null
        threads.add(thread {
            scoreAddTileToPrisonBus = evaluateAddTileToBus.getScoreAddTileToPrisonBus(game, depth, maximize, actionsChecked)
        })
        threads.add(thread {
            scoreMoveOwnPrisoner = evaluateMoveOwnPrisoner.getScoreMoveOwnPrisoner(game, depth, maximize, actionsChecked)
        })
        threads.add(thread {
            scoreMoveEmployee = evaluateMoveEmployee.getScoreMoveEmployee(game, depth, maximize, actionsChecked)
        })
        threads.add(thread {
            scoreBuyPrisoner = evaluateBuyPrisoner.getScoreBuyPrisoner(game, depth, maximize, actionsChecked)
        })
        threads.add(thread {
            scoreFreePrisoner = evaluateActionFreePrisoner.freePrisoner(game, depth, maximize, actionsChecked)
        })
        threads.add(thread {
            scoreExpandPrison = evaluateExpandPrison.getScoreExpandPrisonGrid(game, depth, maximize, actionsChecked)
        })
        threads.add(thread {
            scoreTakeBus = evaluateActionTakeBus.takeBus(game, depth, maximize, actionsChecked)
        })

        Thread.sleep(8000)
        for (t in threads) {
            t.interrupt()
        }
        Thread.sleep(1000)

        val scoreListNulls = mutableListOf(scoreAddTileToPrisonBus, scoreMoveOwnPrisoner, scoreMoveEmployee,
            scoreBuyPrisoner, scoreFreePrisoner, scoreExpandPrison, scoreTakeBus)

        return this.getBestAction(maximize, scoreListNulls.filterNotNull(), game)
    }

    /**
     * Function to get the best action depending on the current value of maximize
     * If maximize % playerCount is 0 the value should be maximized, because this would be the turn
     * of the AI, if maximize % playerCount is not 0 this would mean another AI/player is making this
     * turn and the lowest possible value is taken (because we assume the other players are always making the
     * best decision)
     *
     * @param maximize the player taking the turn, 0 is the AI making the turn
     * @param actionList a list of possible actions the AI/player could make
     * @param game the current game
     * @return the action this AI/player should/would perform
     */
    fun getBestAction(maximize: Int, actionList: List<AIAction> , game: AquaGhetto): AIAction {
        return if ((maximize % game.players.size) == 0) {
            var bestScore = Integer.MIN_VALUE
            var bestAction: AIAction? = null
            for (element in actionList) {
                if (element.score > bestScore && element.validAction) {
                    bestAction = element
                    bestScore = element.score
                }
            }
            bestAction ?: AIAction(false,0)
        } else {
            var bestScore = Integer.MAX_VALUE
            var bestAction: AIAction? = null
            for (element in actionList) {
                if (element.score < bestScore && element.validAction) {
                    bestAction = element
                    bestScore = element.score
                }
            }
            bestAction ?: AIAction(false,0)
        }
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
            //TODO add emergency action?
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






}


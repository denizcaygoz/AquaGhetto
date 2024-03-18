package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionExpandPrison
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import service.aiServices.smart.SmartAI
import kotlin.math.abs
/**
 * Class that stores the function that is necessary
 * to simulate the action of expand prison grid.
 */
class EvaluateExpandPrisonGridService(private val smartAI: SmartAI) {

    private val nextTo = mutableListOf(Pair(0,1),Pair(1,0),Pair(0,-1),Pair(-1,0))
    private val nextToFurther = mutableListOf(
        Pair(0,2),Pair(-1,1),Pair(0,1),Pair(1,1),
        Pair(-2,0),Pair(-1,0),Pair(1,0),Pair(1,0),
        Pair(0,-2),Pair(-1,-1),Pair(0,-1),Pair(1,-1)
    )

    fun getScoreExpandPrisonGrid(game: AquaGhetto, depth: Int): ActionExpandPrison {
        val player = game.players[game.currentPlayer]

        val bigExtension = this.bigExpand(game, depth, player)
        val smallExtension = this.smallExpand(game, depth, player)

        if (bigExtension == null && smallExtension == null) {
            return ActionExpandPrison(false, 0, false, Pair(0,0) , 0)
        } else if (bigExtension == null && smallExtension != null) {
            return smallExtension
        } else if (bigExtension != null && smallExtension == null) {
            return bigExtension
        }

        /*smart cast does not know that at this stage smallExtension != null and bigExtension != null*/
        if (smallExtension == null || bigExtension == null) {
            return ActionExpandPrison(false, 0, false, Pair(0,0) , 0)
        }

        return if (smallExtension.score >= bigExtension.score) {
            smallExtension
        } else {
            bigExtension
        }
    }

    private fun bigExpand(game: AquaGhetto, depth: Int, player: Player): ActionExpandPrison? {
        if (player.coins < 2) return null
        if (player.remainingBigExtensions <= 0) return null

        val bestPos = this.getBestLocation(game, player, true) ?: return null

        player.remainingBigExtensions -= 1
        player.coins -= 2

        val nextPlayer = smartAI.getNextAndOldPlayer(game,false)
        game.currentPlayer = nextPlayer.second

        val undoData = simulatePlaceExtension(bestPos, true, player)

        val bestAction = smartAI.minMax(game, depth)

        undoSimulatePlaceExtension(player, undoData)

        game.currentPlayer = nextPlayer.first
        player.coins += 2
        player.remainingBigExtensions += 1

        return ActionExpandPrison(true, bestAction.score, true,
            Pair(bestPos.first,bestPos.second), bestPos.third)
    }

    private fun smallExpand(game: AquaGhetto, depth: Int, player: Player): ActionExpandPrison? {
        if (player.coins < 1) return null
        if (player.remainingSmallExtensions <= 0) return null

        val bestPos = this.getBestLocation(game, player, false) ?: return null

        player.remainingSmallExtensions -= 1
        player.coins -= 1

        val nextPlayer = smartAI.getNextAndOldPlayer(game,false)
        game.currentPlayer = nextPlayer.second

        val undoData = simulatePlaceExtension(bestPos, false, player)

        val bestAction = smartAI.minMax(game, depth)

        undoSimulatePlaceExtension(player, undoData)

        game.currentPlayer = nextPlayer.first
        player.coins += 1
        player.remainingSmallExtensions += 1

        return ActionExpandPrison(true, bestAction.score, false,
            Pair(bestPos.first,bestPos.second), bestPos.third)
    }

    private fun simulatePlaceExtension(placement: Triple<Int, Int, Int>, isBig: Boolean,
                                       player: Player): List<Pair<Int, Int>> {
        val cords = createLocations(placement, isBig)
        for (pos in cords) {
            player.board.setPrisonGrid(pos.first, pos.second, true)
        }
        return cords
    }

    private fun undoSimulatePlaceExtension(player: Player, posList: List<Pair<Int, Int>>) {
        for (pos in posList) {
            player.board.setPrisonGrid(pos.first, pos.second, false)
        }
    }

    private fun getBestLocation(game: AquaGhetto, player: Player, isBig: Boolean): Triple<Int,Int,Int>? {
        val validPlacements = mutableListOf<Triple<Int,Int,Int>>()

        /*get all grid locations surrounding the outer grid*/
        val borderTiles = mutableSetOf<Pair<Int,Int>>()
        for (firstIterator in player.board.getPrisonGridIterator()) {
            for (secondIterator in firstIterator.value) {
                borderTiles.add(Pair(firstIterator.key + 0, secondIterator.key + 1))
                borderTiles.add(Pair(firstIterator.key + 0, secondIterator.key - 1))
                borderTiles.add(Pair(firstIterator.key + 1, secondIterator.key + 0))
                borderTiles.add(Pair(firstIterator.key - 1, secondIterator.key + 0))

                borderTiles.add(Pair(firstIterator.key + 1, secondIterator.key + 1))
                borderTiles.add(Pair(firstIterator.key - 1, secondIterator.key - 1))
                borderTiles.add(Pair(firstIterator.key + 1, secondIterator.key - 1))
                borderTiles.add(Pair(firstIterator.key - 1, secondIterator.key + 1))
            }
        }

        /*iterate through all border tiles*/
        for (location in borderTiles) {
            /*remove locations, where there is a grid*/
            if (player.board.getPrisonGrid(location.first, location.second)) continue

            /*add grid location if valid*/
            addExtensionPlacesToList(validPlacements, location.first, location.second, isBig)
        }

        val cardsInGame = smartAI.rootService.boardService.getCardsStillInGame().second
        val playerCards = smartAI.rootService.evaluationService.getPrisonerTypeCount(player)
        this.addCardOnBus(cardsInGame, game)

        val posScored = mutableListOf<Pair<Triple<Int,Int,Int>,Int>>()
        for (placement in validPlacements) {
            posScored.add(Pair(placement, this.evaluatePlacement(placement, isBig, player, cardsInGame, playerCards)))
        }

        if (posScored.isEmpty()) return null

        return posScored.maxBy { it.second }.first
    }


    private fun addExtensionPlacesToList(list: MutableList<Triple<Int,Int,Int>>
                                         , x: Int, y: Int, isBig: Boolean) {
        if (isBig) {
            if (smartAI.rootService.validationService.validateExpandPrisonGrid(true, x , y , 0)) {
                list.add(Triple(x,y,0))
            }
            return
        }

        /*basic pre-check*/
        if (x == 0 && y == 0) return
        if (x == 1 && y == 0) return
        if (x == 0 && y == 1) return

        /*all possible rotations*/
        val validRotations = mutableListOf(0,90,180,270)

        /*check every rotation if it is valid*/
        for (rot in validRotations) {
            if (!smartAI.rootService.validationService.validateExpandPrisonGrid(false, x , y , rot)) continue
            list.add(Triple(x,y,rot))
        }
    }

    private fun createLocations(placement: Triple<Int, Int, Int>, isBig: Boolean): MutableList<Pair<Int, Int>> {
        val x = placement.first
        val y = placement.second
        val rotation = placement.third

        val placementCoordinates: MutableList<Pair<Int, Int>> = mutableListOf()
        placementCoordinates.add(Pair(x,y))

        if (isBig) {
            placementCoordinates.add(Pair(x+1,y))
            placementCoordinates.add(Pair(x,y-1))
            placementCoordinates.add(Pair(x+1,y-1))
        } else {
            when(rotation) {
                0 -> {
                    placementCoordinates.add(Pair(x,y-1))
                    placementCoordinates.add(Pair(x+1,y-1))
                }
                90 -> {
                    placementCoordinates.add(Pair(x-1,y))
                    placementCoordinates.add(Pair(x-1,y-1))
                }
                180 -> {
                    placementCoordinates.add(Pair(x,y+1))
                    placementCoordinates.add(Pair(x-1,y+1))
                }
                270 -> {
                    placementCoordinates.add(Pair(x+1,y+1))
                    placementCoordinates.add(Pair(x+1,y))
                }
            }
        }
        return placementCoordinates
    }

    private fun evaluatePlacement(placement: Triple<Int,Int,Int>, isBig: Boolean, player: Player,
                                  cardsInGame: MutableMap<PrisonerType, Int>,
                                  playerCards: MutableMap<PrisonerType, Int>): Int {
        val x = placement.first
        val y = placement.second

        /*bigger circumference -> better*/
        val placementCoordinates = createLocations(placement, isBig)

        var baseScore = getNonAdjacentGrid(player, placementCoordinates, isBig) * 5

        if (!isBig) {
            /*if not big used to expand an existing type, next to a type*/
            /*extension is not needed if mostNeeded is null*/
            val mostNeeded = getMostNeededExtensionForType(playerCards, cardsInGame, player, false) ?: return -10

            /*location is not good if not next to the needed type*/
            if (!isNextToSpecifiedType(player, placementCoordinates, mostNeeded)) return -10
        } else {
            /*better for a new type, should be far away from other types*/
            if (player.maxPrisonerTypes > countPlayerTypes(playerCards)) {
                /*big as extension because no space*/
                val mostNeeded = getMostNeededExtensionForType(playerCards, cardsInGame, player, true) ?: return -10

                /*location is not good if not next to the needed type*/
                if (!isNextToSpecifiedType(player, placementCoordinates, mostNeeded)) return 0
            } else {
                /*big because more types place far away from other types*/
                var averageDistance = 0.0
                for (iterator in playerCards) {
                    averageDistance += calcDistanceToDifferentType(player, iterator.key, x, y)
                }
                averageDistance /= countPlayerTypes(playerCards)
                baseScore -= averageDistance.toInt()
            }
        }

        return baseScore
    }

    private fun countPlayerTypes(map: MutableMap<PrisonerType, Int>): Int {
        var count = 0
        for (iterator in map) {
            if (iterator.value != 0) count++
        }
        return count
    }

    private fun isNextToSpecifiedType(player: Player, posList: List<Pair<Int,Int>>, prisonerType: PrisonerType): Boolean {
        for (pos in posList) {
            for (offset in nextTo) {
                val type = player.board.getPrisonYard(pos.first + offset.first, pos.second + offset.second)
                if (type != null && type is PrisonerTile && type.prisonerType == prisonerType) return true
            }
        }
        return false
    }

    private fun getNonAdjacentGrid(player: Player, posList: List<Pair<Int,Int>>, isBig: Boolean): Int {
        var count = 0
        for (pos in posList) {
            for (offset in nextTo) {
                if (!player.board.getPrisonGrid(pos.first + offset.first, pos.second + offset.second)) count++
            }
        }
        count -= if (isBig) 4 else 2
        return count
    }

    private fun getAmountPossiblePlacesForCardDistance(player: Player, type: PrisonerType): Int {
        var validPos = 0
        for (firstIterator in player.board.getPrisonYardIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = player.board.getPrisonYard(firstIterator.key, secondIterator.key)
                if (tile == null || tile !is PrisonerTile || tile.prisonerType != type) continue
                validPos += this.getAmountPossibleCard(player, type, firstIterator.key, secondIterator.key)
            }
        }
        return validPos
    }

    private fun getAmountPossibleCard(player: Player, type: PrisonerType, x: Int, y: Int): Int {
        var validPos = 0
        /*get all valid options and check adjacent grid spaces -> more adjacent grid -> more blocked -> not good*/
        for (offset in nextToFurther) {
            if (this.validateTilePlacement(player, type, x + offset.first, y + offset.second)) {
                validPos++
            }
        }
        return validPos
    }

    /*validation function with fewer checks*/
    private fun validateTilePlacement(player: Player, prisonerType: PrisonerType, x:Int, y:Int): Boolean {
        if (player.board.getPrisonYard(x,y) != null) return false
        if (!player.board.getPrisonGrid(x,y)) return false
        for (offset in nextTo) {
            val tileCheck = player.board.getPrisonYard(x + offset.first, y + offset.second) ?: continue
            if (tileCheck is PrisonerTile && (tileCheck.prisonerType != prisonerType)) {
                return false
            }
        }
        return true
    }

    /*if cards are already on the bus they will count as 2 cards*/
    private fun addCardOnBus(map: MutableMap<PrisonerType, Int>, game: AquaGhetto) {
        for (bus in game.prisonBuses) {
            for (tile in bus.tiles) {
                if (tile == null || tile !is PrisonerTile) continue
                val amount = (map[tile.prisonerType] ?: 0) + 2
                map[tile.prisonerType] = amount
            }
        }
    }

    private fun getMostNeededExtensionForType(playerCards: MutableMap<PrisonerType,Int>,
                                              cardsInGame: MutableMap<PrisonerType,Int>,
                                              player: Player, isBig: Boolean): PrisonerType? {
        /*int is "how much" this is needed*/
        val neededTypes = mutableSetOf<Pair<PrisonerType,Int>>()
        for (playerCard in playerCards) {
            val type = playerCard.key

            var amountInGame = cardsInGame[type] ?: 0
            if (isBig) amountInGame -= 2 /*big extension is more useful if more cards of this type*/
            if (amountInGame == 0) continue


            val possiblePlacesForType = this.getAmountPossiblePlacesForCardDistance(player, type)
            if (possiblePlacesForType > 2) continue /*extension not useful*/

            neededTypes.add(Pair(type, amountInGame))
        }

        if (neededTypes.isEmpty()) return null

        return neededTypes.maxBy { it.second }.first
    }

    private fun calcDistanceToDifferentType(player: Player, type: PrisonerType, x: Int, y: Int): Int {
        var smallestDistance = Integer.MAX_VALUE
        for (firstIterator in player.board.getPrisonYardIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = secondIterator.value
                if (tile !is PrisonerTile || tile.prisonerType == type) continue
                val dist = abs(x - firstIterator.key) + abs(y - secondIterator.key)
                if (dist < smallestDistance) {
                    smallestDistance = dist
                }
            }
        }
        return smallestDistance
    }


}
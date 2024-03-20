package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.PrisonBus
import entity.aIActions.AIAction
import entity.aIActions.ActionAddTileToBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.aiServices.smart.SmartAI
import kotlin.math.floor

/**
 * Class that stores the function that is necessary
 * to simulate the action of add tile to prison bus.
 */
class EvaluateAddTileToPrisonBusService(private val smartAI: SmartAI) {
    /**
     * simulates the add tile to Prison Bus action. Called by minmax function in SmartAI class.
     */
    fun getScoreAddTileToPrisonBus(game: AquaGhetto, depth: Int): ActionAddTileToBus {
        /*by simulation all possible tiles being drawn (trait is ignored) the AI
        has no advantage compared to a normal player*/

        val tilesLeftInGame = smartAI.rootService.boardService.getCardsStillInGame()
        val allCardsAmount = game.drawStack.size + game.finalStack.size

        val coinsLeft = tilesLeftInGame.first
        val prisonerLeft = tilesLeftInGame.second

        val prisonBusesLeftToPlace = mutableListOf<Int>()
        for (i in game.prisonBuses.indices) {
            if (checkIfBusIsValid(game.prisonBuses[i])) {
                prisonBusesLeftToPlace.add(i)
            }
        }


        var totalScore = 0.0
        var chooseBusCoin =  -1
        var validOption = false


        if (coinsLeft > 0) {
            val coinAction = this.simulateCoinTileWasDrawn(game, prisonBusesLeftToPlace, depth,
                (coinsLeft / (allCardsAmount.toDouble()))
            )
            if (coinAction.first) {
                totalScore += coinAction.second
                chooseBusCoin = coinAction.third
                validOption = true
            }
        }


        val optionMapPrisoner = mutableMapOf<PrisonerType,Int>()
        for (prisoner in prisonerLeft) {
            val prisonerAction = this.simulatePrisonerTileWasDrawn(game, prisonBusesLeftToPlace, depth,
                 prisoner.key, (prisoner.value / (allCardsAmount.toDouble()))
            )
            if (prisonerAction.first) {
                totalScore += prisonerAction.second
                optionMapPrisoner[prisoner.key] = prisonerAction.third
                validOption = true
            }
        }

        //TODO decrease the amount of options

        return ActionAddTileToBus(validOption, totalScore.toInt(), chooseBusCoin, optionMapPrisoner)
    }

    /**
     * check if the player can place a tile to the bus that is available.
      */
    private fun checkIfBusIsValid(prisonBus: PrisonBus): Boolean {
        for (i in prisonBus.tiles.indices) {
            if (prisonBus.tiles[i] == null && !prisonBus.blockedSlots[i]) return true
        }
        return false
    }

    /**
     * simulates if the drawn card is a coin.
     */
    private fun simulateCoinTileWasDrawn(game: AquaGhetto, prisonBusesLeftToPlace: MutableList<Int>,
                                         depth: Int, mult: Double): Triple<Boolean, Double , Int> {
        val allCardsLeft = mutableListOf<Tile>()
        allCardsLeft.addAll(game.finalStack)
        allCardsLeft.addAll(game.drawStack)

        /*simulates the occurrence of a coin tile, removes the card and inserts it again later at the same position*/
        /*this means that the AI has no advantage over other players*/
        /*
        var index = -1
        for (i in allCardsLeft.indices) {
            if (allCardsLeft[i] is CoinTile) index = i
        }

        if (index == -1) return Triple(false, 0.0, 0)

        val tile = if (index < 15) {
            println("$index      ${game.finalStack.size}")
            game.finalStack.removeAt(index)
        } else {
            game.drawStack.removeAt(index - 15)
        }
        */

        /*simulates future actions*/
        var bestBus = -1
        var bestAction: AIAction? = null
        for (i in prisonBusesLeftToPlace) {
            val nextPlayer = smartAI.getNextAndOldPlayer(game, false)
            game.currentPlayer = nextPlayer.second

            val tile = CoinTile(-10123)
            addTileToBus(game.prisonBuses[i] , tile)

            val action = smartAI.minMax(game, depth)

            removeTileFromBus(game.prisonBuses[i] , tile)

            game.currentPlayer = nextPlayer.first
            if (bestAction == null || (action.score > bestAction.score)) {
                bestAction = action
                bestBus = i
            }
        }

        /*undo action*/
        /*
        if (index < 15) {
            game.finalStack.add(index, tile)
        } else {
            game.drawStack.add(index - 15, tile)
        }
        */

        return if (bestBus == -1 || bestAction == null) {
            Triple(false, 0.0, 0)
        } else {
            Triple(bestAction.validAction, bestAction.score * mult, bestBus)
        }

    }

    /**
     * simulates if the drawn card is a tile.
     */
    private fun simulatePrisonerTileWasDrawn(game: AquaGhetto, prisonBusesLeftToPlace: MutableList<Int>, depth: Int,
                                             prisonerType: PrisonerType, mult: Double): Triple<Boolean, Double , Int> {
        val allCardsLeft = mutableListOf<Tile>()
        allCardsLeft.addAll(game.finalStack)
        allCardsLeft.addAll(game.drawStack)

        /*simulates the occurrence of a prisoner tile with the specified type,
        removes the card and inserts it again later at the same position*/
        /*this means that the AI has no advantage over other players*/
        /*
        var index = -1
        for (i in allCardsLeft.indices) {
            val card = allCardsLeft[i]
            if (card is PrisonerTile && card.prisonerType == prisonerType) index = i
        }

        if (index == -1) return Triple(false, 0.0, 0)

        val tile = if (index < 15) {
            game.finalStack.removeAt(index)
        } else {
            game.drawStack.removeAt(index - 15)
        }
        */

        /*simulates future actions*/
        var bestBus = -1
        var bestAction: AIAction? = null

        for (i in prisonBusesLeftToPlace) {
            val nextPlayer = smartAI.getNextAndOldPlayer(game, false)
            game.currentPlayer = nextPlayer.second

            val tile = PrisonerTile(-10123, PrisonerTrait.NONE, prisonerType)

            addTileToBus(game.prisonBuses[i] , tile)

            val action = smartAI.minMax(game, depth)

            removeTileFromBus(game.prisonBuses[i] , tile)

            game.currentPlayer = nextPlayer.first
            if (bestAction == null || (action.score > bestAction.score)) {
                bestAction = action
                bestBus = i
            }
        }

        /*undo action*/
        /*
        if (index < 15) {
            game.finalStack.add(index, tile)
        } else {
            game.drawStack.add(index - 15, tile)
        }
        */

        return if (bestBus == -1 || bestAction == null) {
            Triple(false, 0.0, 0)
        } else {
            Triple(bestAction.validAction, bestAction.score * mult, bestBus)
        }
    }

    /**
     * adds the tile to the first slot of the bus that is available.
     */
    private fun addTileToBus(bus: PrisonBus, tile: Tile) {
        for (i in bus.tiles.indices) {
            if (bus.tiles[i] == null && !bus.blockedSlots[i]) {
                bus.tiles[i] = tile
                return
            }
        }
    }

    /**
     * removes the tile to the first slot of the bus that is available.
     */
    private fun removeTileFromBus(bus: PrisonBus, tile: Tile) {
        for (i in bus.tiles.indices) {
            val busTile = bus.tiles[i]
            if (busTile != null && busTile.id == tile.id) {
                bus.tiles[i] = null
            }
        }
    }


}
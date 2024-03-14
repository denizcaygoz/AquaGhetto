package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.PrisonBus
import entity.aIActions.ActionAddTileToBus
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.aiServices.smart.SmartAI

class EvaluateAddTileToPrisonBusService(private val smartAI: SmartAI) {

    fun getScoreAddTileToPrisonBus(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionAddTileToBus {

        val tilesLeftInGame = smartAI.rootService.boardService.getCardsStillInGame()

        val coinsLeft = tilesLeftInGame.first
        val prisonerLeft = tilesLeftInGame.second

        val prisonBusesLeftToPlace = mutableListOf<Int>()
        for (i in game.prisonBuses.indices) {
            if (checkIfBusIsValid(game.prisonBuses[i])) {
                prisonBusesLeftToPlace.add(i)
            }
        }

        val validOptions = mutableListOf<ActionAddTileToBus>()

        val coinAction = this.simulateCoinTileWasDrawn(game, prisonBusesLeftToPlace, depth, maximize, amountActions)
        coinAction.score *= coinsLeft
        if (coinAction.validAction) validOptions.add(coinAction)

        for (prisoner in prisonerLeft) {
            val prisonerAction = this.simulatePrisonerTileWasDrawn(game, prisonBusesLeftToPlace, depth,
                maximize, amountActions, prisoner.key)
            prisonerAction.score *= prisoner.value
            if (prisonerAction.validAction) validOptions.add(prisonerAction)
        }

        val bestAction = smartAI.getBestAction(maximize, validOptions, game)

        return if (bestAction is ActionAddTileToBus) {
            bestAction
        } else {
            ActionAddTileToBus(false, 0, 0)
        }

    }

    private fun checkIfBusIsValid(prisonBus: PrisonBus): Boolean {
        for (i in prisonBus.tiles.indices) {
            if (prisonBus.tiles[i] != null && !prisonBus.blockedSlots[i]) return true
        }
        return false
    }

    private fun simulateCoinTileWasDrawn(game: AquaGhetto, prisonBusesLeftToPlace: MutableList<Int>,
                                         depth: Int, maximize: Int, amountActions: Int): ActionAddTileToBus {
        val allCardsLeft = mutableListOf<Tile>()
        allCardsLeft.addAll(game.finalStack)
        allCardsLeft.addAll(game.drawStack)

        var index = -1
        for (i in allCardsLeft.indices) {
            if (allCardsLeft[i] is CoinTile) index = i
        }

        if (index == -1) return ActionAddTileToBus(false, 0,0)

        val tile = if (index < 15) {
            game.finalStack.removeAt(index)
        } else {
            game.drawStack.removeAt(index)
        }

        var bestBus = -1
        var best: Int
        if (maximize % game.players.size == 0) {
            best = Integer.MIN_VALUE

            for (i in prisonBusesLeftToPlace) {
                val action = smartAI.minMax(game, depth, maximize, amountActions)
                if (action.validAction && (action.score > best)) {
                    best = action.score
                    bestBus = i
                }
            }

        } else {
            best = Integer.MAX_VALUE

            for (i in prisonBusesLeftToPlace) {
                val action = smartAI.minMax(game, depth, maximize, amountActions)
                if (action.validAction && (action.score < best)) {
                    best = action.score
                    bestBus = i
                }
            }
        }

        val result = if (bestBus == -1) {
            /*found no valid actions*/
            ActionAddTileToBus(false, 0,0)
        } else {
            /*return best option*/
            ActionAddTileToBus(true, best,bestBus)
        }

        /*undo action*/
        if (index < 15) {
            game.finalStack.add(index, tile)
        } else {
            game.drawStack.add(index, tile)
        }

        return result
    }

    private fun simulatePrisonerTileWasDrawn(game: AquaGhetto, prisonBusesLeftToPlace: MutableList<Int>,
                                         depth: Int, maximize: Int, amountActions: Int,
                                             prisonerType: PrisonerType): ActionAddTileToBus {
        val allCardsLeft = mutableListOf<Tile>()
        allCardsLeft.addAll(game.finalStack)
        allCardsLeft.addAll(game.drawStack)

        var index = -1
        for (i in allCardsLeft.indices) {
            val card = allCardsLeft[i]
            if (card is PrisonerTile && card.prisonerType == prisonerType) index = i
        }

        if (index == -1) return ActionAddTileToBus(false, 0,0)

        val tile = if (index < 15) {
            game.finalStack.removeAt(index)
        } else {
            game.drawStack.removeAt(index)
        }

        var bestBus = -1
        var best: Int
        if (maximize % game.players.size == 0) {
            best = Integer.MIN_VALUE

            for (i in prisonBusesLeftToPlace) {
                val action = smartAI.minMax(game, depth, maximize, amountActions)
                if (action.validAction && (action.score > best)) {
                    best = action.score
                    bestBus = i
                }
            }

        } else {
            best = Integer.MAX_VALUE

            for (i in prisonBusesLeftToPlace) {
                val action = smartAI.minMax(game, depth, maximize, amountActions)
                if (action.validAction && (action.score < best)) {
                    best = action.score
                    bestBus = i
                }
            }
        }

        val result = if (bestBus == -1) {
            /*found no valid actions*/
            ActionAddTileToBus(false, 0,0)
        } else {
            /*return best option*/
            ActionAddTileToBus(true, best,bestBus)
        }

        /*undo action*/
        if (index < 15) {
            game.finalStack.add(index, tile)
        } else {
            game.drawStack.add(index, tile)
        }

        return result
    }




}
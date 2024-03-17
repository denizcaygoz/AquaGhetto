package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.PrisonBus
import entity.aIActions.ActionAddTileToBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.aiServices.smart.SmartAI

class EvaluateAddTileToPrisonBusService(private val smartAI: SmartAI) {

    fun getScoreAddTileToPrisonBus(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionAddTileToBus {
        /*by simulation all possible tiles being drawn (trait is ignored) the AI
        has no advantage compared to a normal player*/

        var foundTile = false
        for (bus in game.prisonBuses) {
            for (a in bus.tiles) {
                if (a != null) {
                    foundTile = true
                    break
                }
            }
        }

        /*
        if (foundTile) {
            for (bus in game.prisonBuses) {
                println("abc1: ${bus.tiles.contentToString()} ${bus.blockedSlots.contentToString()}   $bus")
            }
        }
        */


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
                maximize, amountActions, coinsLeft / allCardsAmount.toDouble())
            if (coinAction.first) {
                totalScore += coinAction.second
                chooseBusCoin = coinAction.third
                validOption = true
            }
        }




        val optionMapPrisoner = mutableMapOf<PrisonerType,Int>()
        for (prisoner in prisonerLeft) {
            val prisonerAction = this.simulatePrisonerTileWasDrawn(game, prisonBusesLeftToPlace, depth,
                maximize, amountActions, prisoner.key, prisoner.value / allCardsAmount.toDouble())
            if (prisonerAction.first) {
                totalScore += prisonerAction.second
                optionMapPrisoner[prisoner.key] = prisonerAction.third
                validOption = true
            }
        }


        /*
        if (foundTile) {
            for (bus in game.prisonBuses) {
                println("abc2: ${bus.tiles.contentToString()} ${bus.blockedSlots.contentToString()}   $bus")
            }
        }
        */

        return ActionAddTileToBus(validOption, totalScore.toInt(), chooseBusCoin, optionMapPrisoner)
        //return ActionAddTileToBus(false, 0, 0, mutableMapOf())
    }


    private fun checkIfBusIsValid(prisonBus: PrisonBus): Boolean {
        for (i in prisonBus.tiles.indices) {
            if (prisonBus.tiles[i] == null && !prisonBus.blockedSlots[i]) return true
        }
        return false
    }

    private fun simulateCoinTileWasDrawn(game: AquaGhetto, prisonBusesLeftToPlace: MutableList<Int>,
                                         depth: Int, maximize: Int, amountActions: Int, mult: Double): Triple<Boolean, Double , Int> {
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
        var best = Integer.MIN_VALUE
        for (i in prisonBusesLeftToPlace) {
            val nextPlayer = smartAI.getNextAndOldPlayer(game, false)
            game.currentPlayer = nextPlayer.second

            val tile = CoinTile(-10123)
            addTileToBus(game.prisonBuses[i] , tile)

            val action = smartAI.minMax(game, depth, maximize, amountActions)

            removeTileFromBus(game.prisonBuses[i] , tile)

            game.currentPlayer = nextPlayer.first
            if ((action.score > best)) {
                best = action.score
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

        return if (bestBus == -1) {
            Triple(false, 0.0, 0)
        } else {
            Triple(true, best * mult, bestBus)
        }

    }

    private fun simulatePrisonerTileWasDrawn(game: AquaGhetto, prisonBusesLeftToPlace: MutableList<Int>,
                                         depth: Int, maximize: Int, amountActions: Int,
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
        var best = Integer.MIN_VALUE

        for (i in prisonBusesLeftToPlace) {
            val nextPlayer = smartAI.getNextAndOldPlayer(game, false)
            game.currentPlayer = nextPlayer.second

            val tile = PrisonerTile(-10123, PrisonerTrait.NONE, prisonerType)

            addTileToBus(game.prisonBuses[i] , tile)

            val action = smartAI.minMax(game, depth, maximize, amountActions)

            removeTileFromBus(game.prisonBuses[i] , tile)

            game.currentPlayer = nextPlayer.first
            if ((action.score > best)) {
                best = action.score
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

        return if (bestBus == -1) {
            Triple(false, 0.0, 0)
        } else {
            Triple(true, best * mult, bestBus)
        }
    }

    private fun addTileToBus(bus: PrisonBus, tile: Tile) {
        for (i in bus.tiles.indices) {
            if (bus.tiles[i] == null && !bus.blockedSlots[i]) {
                bus.tiles[i] = tile
                return
            }
        }
    }

    private fun removeTileFromBus(bus: PrisonBus, tile: Tile) {
        for (i in bus.tiles.indices) {
            val busTile = bus.tiles[i]
            if (busTile != null) {
                //println("id tile: ${busTile.id}")
            }
            if (busTile != null && busTile.id == tile.id) {
                //if (busTile.id != -10123) println("Removed Tile!!!!!!!!!!!!!!!!!!")
                bus.tiles[i] = null
            }
        }
    }


}
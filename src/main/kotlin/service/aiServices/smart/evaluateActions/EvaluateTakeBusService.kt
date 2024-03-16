package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionTakeBus
import entity.aIActions.PlaceCard
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import service.aiServices.smart.SmartAI

class EvaluateTakeBusService(private val smartAI: SmartAI) {

    fun takeBus(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionTakeBus {

        val bestActions = mutableListOf<ActionTakeBus>()
        for (busIndex in game.prisonBuses.indices) {
            bestActions.add(simulateTakeBus(game, depth, maximize, amountActions, game.players[game.currentPlayer], busIndex))
        }

        return bestActions.maxBy { it.score }
    }

    private fun simulateTakeBus(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int
                                , player: Player, busIndex: Int): ActionTakeBus {
        val bus = game.prisonBuses.removeAt(busIndex)
        player.takenBus = bus

        var coins = 0
        val bestPos = mutableListOf<Pair<PrisonerTile, Pair<PlaceCard, Boolean>>>()
        for (card in bus.tiles) {
            if (card == null) {
                continue
            } else if (card is CoinTile) {
                coins++
            } else if (card is PrisonerTile) {
                val pos = smartAI.evaluateBestPosition.getBestPositions(card, player)
                if (pos != null) {
                    bestPos.add(Pair(card, Pair(pos.first, pos.second)))
                } else {
                    bestPos.add(Pair(card, Pair(PlaceCard(Pair(-101,-101)), false)))
                }
            }
        }
        player.coins += coins

        val undoes = mutableListOf<Pair<PrisonerTile, PrisonerTile>?>()
        for (pos in bestPos) {
            undoes.add(smartAI.simulatePlacement(pos.second.first, pos.first, pos.second.second, player))
        }

        //sim future
        val bestAction = smartAI.minMax(game, depth, maximize, amountActions)

        /*undo stuff*/
        player.coins -= coins
        for (i in bestPos.indices) {
            smartAI.undoSimulatePlacement(bestPos[i].second.first, bestPos[i].second.second, player, undoes[i])
        }
        game.prisonBuses.add(busIndex, bus)
        player.takenBus = null

        val cardPos = mutableListOf<PlaceCard>()
        for (pos in bestPos) {
            cardPos.add(pos.second.first)
        }

        return ActionTakeBus(true, bestAction.score, busIndex , cardPos)
    }


}
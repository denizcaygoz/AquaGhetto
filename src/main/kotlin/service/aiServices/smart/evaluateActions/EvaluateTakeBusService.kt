package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionTakeBus
import entity.aIActions.PlaceCard
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import service.aiServices.smart.SmartAI
/**
 * Class that stores the function that is necessary
 * to simulate the action of take bus.
 */
class EvaluateTakeBusService(private val smartAI: SmartAI) {
    /**
     * tries to take the best bus, so that the AI gets the most point.
     * First finds the busses that is available to take. the calls simulateTakeBus.
     * simulateTakeBus return the best action with the most point.
     */
    fun takeBus(game: AquaGhetto, depth: Int): ActionTakeBus {

        val bestActions = mutableListOf<ActionTakeBus>()
        for (busIndex in game.prisonBuses.indices) {
            val action = simulateTakeBus(game, depth, game.players[game.currentPlayer], busIndex)
            if (!action.validAction) continue
            bestActions.add(action)
        }

        if (bestActions.isEmpty()) return ActionTakeBus(false, 0 , 0, mutableListOf())

        return bestActions.maxBy { it.score }
    }

    /**
     * Function checks for each bus and evaluates what will happen when the tiles on the buses is taken, by
     * finding for each tile the best position to place.
     * After this simulated undo stuff is called.
     */
    private fun simulateTakeBus(game: AquaGhetto, depth: Int, player: Player, busIndex: Int): ActionTakeBus {

        val bus = game.prisonBuses.removeAt(busIndex)

        if (!bus.tiles.any { it != null }) {
            game.prisonBuses.add(busIndex, bus)
            return ActionTakeBus(false, 0 , 0, mutableListOf())
        }

        player.takenBus = bus

        var coins = 0
        val bestPos = mutableListOf<Pair<PrisonerTile, Pair<PlaceCard, Boolean>>>()
        val undoes = mutableListOf<Pair<PrisonerTile, PrisonerTile>?>()
        for (card in bus.tiles) {
            if (card == null) {
                continue
            } else if (card is CoinTile) {
                coins++
            } else if (card is PrisonerTile) {
                val pos = smartAI.evaluateBestPosition.getBestPositions(card, player, game)
                if (pos != null) {
                    undoes.add(smartAI.simulatePlacement(pos.first, card, pos.second, player))
                    bestPos.add(Pair(card, Pair(pos.first, pos.second)))
                } else {
                    undoes.add(smartAI.simulatePlacement(PlaceCard(Pair(-100,-100)), card, false, player))
                    bestPos.add(Pair(card, Pair(PlaceCard(Pair(-100,-100)), false)))
                }
            }
        }
        player.coins += coins

        val nextPlayer = smartAI.getNextAndOldPlayer(game,true)
        game.currentPlayer = nextPlayer.second

        //sim future
        val bestAction = smartAI.minMax(game, depth)

        game.currentPlayer = nextPlayer.first

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
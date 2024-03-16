package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionBuyPrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI

class EvaluateBuyPrisonerService(val smartAI: SmartAI) {

    fun getScoreBuyPrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionBuyPrisoner {
        val player = game.players[game.currentPlayer]

        val actions = mutableListOf<ActionBuyPrisoner>()
        for (p in game.players) {
            if (p == player) continue
            val action = forOneSpecifiedPlayer(player, p)
            if (action.validAction) actions.add(action)
        }

        /*evaluating all actions would be to expensive I think*/
        if (actions.isEmpty()) return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))

        return actions.maxBy { it.score }
    }

    private fun forOneSpecifiedPlayer(player: Player, buyFrom: Player): ActionBuyPrisoner {
        if (player.coins < 2 || buyFrom.isolation.isEmpty()) {
            return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))
        }

        val removedTile = buyFrom.isolation.pop()
        player.coins -= 2
        buyFrom.coins += 1

        val pos = smartAI.evaluateBestPosition.getBestPositions(removedTile, player)
            ?: return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0))) /*don't buy if no valid place*/

        val undoData = smartAI.simulatePlacement(pos.first, removedTile, pos.second, player)

        val score = smartAI.evaluateGamePosition.evaluateCurrentPosition()

        val result = ActionBuyPrisoner(true, score, buyFrom, pos.first)

        smartAI.undoSimulatePlacement(pos.first, pos.second, player, undoData)

        return result
    }

}
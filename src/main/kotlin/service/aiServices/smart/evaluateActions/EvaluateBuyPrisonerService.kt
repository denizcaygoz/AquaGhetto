package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionBuyPrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI

class EvaluateBuyPrisonerService(private val smartAI: SmartAI) {

    fun getScoreBuyPrisoner(game: AquaGhetto, depth: Int): ActionBuyPrisoner {
        val player = game.players[game.currentPlayer]

        val actions = mutableListOf<ActionBuyPrisoner>()
        for (p in game.players) {
            if (p == player) continue
            val action = forOneSpecifiedPlayer(game, depth, player, p)
            if (action.validAction) actions.add(action)
        }

        if (actions.isEmpty()) return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))

        return actions.maxBy { it.score }
    }

    private fun forOneSpecifiedPlayer(game: AquaGhetto, depth: Int, player: Player, buyFrom: Player): ActionBuyPrisoner {
        if (player.coins < 2 || buyFrom.isolation.isEmpty()) {
            return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))
        }

        val removedTile = buyFrom.isolation.pop()
        player.coins -= 2
        buyFrom.coins += 1

        val pos = smartAI.evaluateBestPosition.getBestPositions(removedTile, player)
            ?: return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0))) /*don't buy if no valid place*/

        val undoData = smartAI.simulatePlacement(pos.first, removedTile, pos.second, player)

        val nextPlayer = smartAI.getNextAndOldPlayer(game, false)
        game.currentPlayer = nextPlayer.second

        val bestAction = smartAI.minMax(game, depth)

        game.currentPlayer = nextPlayer.first

        val result = ActionBuyPrisoner(true, bestAction.score, buyFrom, pos.first)

        player.coins += 2
        buyFrom.coins -= 1
        buyFrom.isolation.add(removedTile)
        smartAI.undoSimulatePlacement(pos.first, pos.second, player, undoData)

        /*buying a prisoner is only useful if the player gets a baby or the player gets a new employee (only useful in late game)*/
        val baby = pos.first.placeBonusPrisoner
        val first = pos.first.firstTileBonusEmployee
        val second = pos.first.secondTileBonusEmployee
        return if (baby != null) {
            result
        } else if ((first != null || second != null) && (game.drawStack.size + game.finalStack.size) < 20) {
            result
        } else {
            ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))
        }

    }

}
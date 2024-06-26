package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionBuyPrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI
/**
 * Class that stores the function that is necessary
 * to simulate the action of buy prisoner from other isolation.
 */
class EvaluateBuyPrisonerService(private val smartAI: SmartAI) {
    /**
     * this function finds the best prisoner and best places to places this
     * prisoner to get the most score.
     */
    fun getScoreBuyPrisoner(game: AquaGhetto, depth: Int): ActionBuyPrisoner {
        //return ActionBuyPrisoner(false, 0 , game.players[game.currentPlayer], PlaceCard(Pair(0,0)))

        val player = game.players[game.currentPlayer]

        val actions = mutableListOf<ActionBuyPrisoner>()
        for (p in game.players) {
            if (p == player || p.name == player.name) continue
            val action = forOneSpecifiedPlayer(game, depth, player, p)
            if (action.validAction) actions.add(action)
        }

        if (actions.isEmpty()) return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))

        return actions.maxBy { it.score }
    }

    /**
     * finds the best position for the prisoner we took from other player's isolation.
     */
    private fun forOneSpecifiedPlayer(game: AquaGhetto, depth: Int, player: Player, buyFrom: Player): ActionBuyPrisoner {
        if (player.coins < 2 || buyFrom.isolation.isEmpty()) {
            return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))
        }

        val removedTile = buyFrom.isolation.pop()
        player.coins -= 2
        buyFrom.coins += 1

        val pos = smartAI.evaluateBestPosition.getBestPositions(removedTile, player, game)

        if (pos == null) {
            buyFrom.coins -= 1
            player.coins += 2
            buyFrom.isolation.add(removedTile)
            return ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0))) /*don't buy if no valid place*/
        }

        val undoData = smartAI.simulatePlacement(pos.first, removedTile, pos.second, player)

        val nextPlayer = smartAI.getNextAndOldPlayer(game, false)
        game.currentPlayer = nextPlayer.second

        val bestAction = smartAI.minMax(game, depth)

        game.currentPlayer = nextPlayer.first

        val result = ActionBuyPrisoner(true, bestAction.score, buyFrom, pos.first)

        player.coins += 2
        buyFrom.coins -= 1
        smartAI.undoSimulatePlacement(pos.first, pos.second, player, undoData)
        buyFrom.isolation.add(removedTile)

        /*buying a prisoner is only useful if the player gets a baby or the player gets a new employee (only useful in late game)*/
        val baby = pos.first.placeBonusPrisoner
        val first = pos.first.firstTileBonusEmployee
        val second = pos.first.secondTileBonusEmployee
        return if (baby != null) {
            result
        } else if ((first != null || second != null) && (game.drawStack.size + game.finalStack.size) < 25) {
            result
        } else {
            ActionBuyPrisoner(false, 0 , player, PlaceCard(Pair(0,0)))
        }

    }

}
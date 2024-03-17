package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionBuyPrisoner
import entity.aIActions.ActionMovePrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI

class EvaluateMoveOwnPrisonerService(val smartAI: SmartAI) {

    fun getScoreMoveOwnPrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionMovePrisoner {
        val player = game.players[game.currentPlayer]

        val action = this.forSimulatePlacePrisoner(game, depth, maximize, amountActions, player)
            ?: return ActionMovePrisoner(false, 0, PlaceCard(Pair(0,0)))

        return action
    }


    /*this option should get rarely used buying extension first is better*/
    private fun forSimulatePlacePrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int, player: Player): ActionMovePrisoner? {
        if (player.coins < 1 || player.isolation.isEmpty()) {
            return null
        }

        val removedTile = player.isolation.pop()
        player.coins -= 1

        /*don't do anything if no valid place*/
        val pos = smartAI.evaluateBestPosition.getBestPositions(removedTile, player)
            ?: return null

        val undoData = smartAI.simulatePlacement(pos.first, removedTile, pos.second, player)

        val nextPlayer = smartAI.getNextAndOldPlayer(game,false)
        game.currentPlayer = nextPlayer.second

        val bestAction = smartAI.minMax(game, depth, maximize, amountActions)

        game.currentPlayer = nextPlayer.first

        val result = ActionMovePrisoner(true, bestAction.score, pos.first)

        player.coins += 1
        player.isolation.add(removedTile)
        smartAI.undoSimulatePlacement(pos.first, pos.second, player, undoData)

        /*buying a prisoner is useful if the player gets a baby*/
        /*in endgame useful if player earns employee or cannot buy extension or this decreases negative points from isolation*/
        val baby = pos.first.placeBonusPrisoner
        val first = pos.first.firstTileBonusEmployee
        val second = pos.first.secondTileBonusEmployee
        return if (baby != null) {
            result
        } else if ((first != null || second != null) && (game.drawStack.size + game.finalStack.size) < 20) {
            result
        } else if ((game.drawStack.size + game.finalStack.size) < 15 && player.lawyerCount <= 1) {
            result
        } else {
            null
        }

    }

}
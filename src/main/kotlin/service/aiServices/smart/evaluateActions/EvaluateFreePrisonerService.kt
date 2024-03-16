package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionFreePrisoner
import service.aiServices.smart.SmartAI

class EvaluateFreePrisonerService(val smartAI: SmartAI) {

    fun freePrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionFreePrisoner {
        val player = game.players[game.currentPlayer]
        if (player.coins < 2 || player.isolation.isEmpty()) {
            return ActionFreePrisoner(false, 0)
        }

        val removedTile = player.isolation.pop()

        val nextPlayer = smartAI.getNextAndOldPlayer(game)
        game.currentPlayer = nextPlayer.second

        val actionFree = smartAI.minMax(game, depth, maximize, amountActions)

        game.currentPlayer = nextPlayer.first
        player.isolation.add(removedTile)

        return ActionFreePrisoner(actionFree.validAction, actionFree.score)
    }

}
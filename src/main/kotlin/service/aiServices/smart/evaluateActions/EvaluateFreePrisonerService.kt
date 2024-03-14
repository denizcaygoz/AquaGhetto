package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionFreePrisoner
import service.aiServices.smart.SmartAI

class EvaluateFreePrisonerService(val smartAI: SmartAI) {

    fun freePrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionFreePrisoner {
        val player = game.players[game.currentPlayer]
        if (player.coins < 2) {
            return ActionFreePrisoner(false, 0)
        }

        val removedTile = player.isolation.pop()
        val actionFree = smartAI.minMax(game, depth, maximize, amountActions)
        player.isolation.add(removedTile)

        return ActionFreePrisoner(actionFree.validAction, actionFree.score)
    }

}
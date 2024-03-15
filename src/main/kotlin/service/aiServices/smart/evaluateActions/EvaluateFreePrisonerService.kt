package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionFreePrisoner
import service.aiServices.smart.SmartAI

/**
 * This class simulates the free Prisoner action.
 */
class EvaluateFreePrisonerService(val smartAI: SmartAI) {

    /**
     * A function to simulate freePrisoner action.
     */
    fun freePrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionFreePrisoner {
        val player = game.players[game.currentPlayer]
        if (player.coins < 2 || player.isolation.isEmpty()) {
            return ActionFreePrisoner(false, 0)
        }
        player.coins -= 2
        val removedTile = player.isolation.pop()
        val actionFree = smartAI.minMax(game, depth, maximize, amountActions)
        player.coins += 2
        player.isolation.add(removedTile)

        return ActionFreePrisoner(actionFree.validAction, actionFree.score)
    }

}
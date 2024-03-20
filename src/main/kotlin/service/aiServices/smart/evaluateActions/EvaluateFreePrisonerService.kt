package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionFreePrisoner
import service.aiServices.smart.SmartAI
/**
 * Class that stores the function that is necessary
 * to simulate the action of free prisoner.
 */
class EvaluateFreePrisonerService(private val smartAI: SmartAI) {
    /**
     * prisoner is taken out from the player. player coins are decremented by 2.
     * next player is determined. minMax calls and after that undo stuff are called.
     */
    fun freePrisoner(game: AquaGhetto, depth: Int): ActionFreePrisoner {

        //TODO
        return ActionFreePrisoner(false, 0)


        val player = game.players[game.currentPlayer]
        if (player.coins < 2 || player.isolation.isEmpty()) {
            return ActionFreePrisoner(false, 0)
        }

        val removedTile = player.isolation.pop()
        player.coins -= 2

        val nextPlayer = smartAI.getNextAndOldPlayer(game,false)
        game.currentPlayer = nextPlayer.second

        val actionFree = smartAI.minMax(game, depth)

        game.currentPlayer = nextPlayer.first
        player.isolation.add(removedTile)
        player.coins += 2

        return ActionFreePrisoner(actionFree.validAction, actionFree.score)
    }

}
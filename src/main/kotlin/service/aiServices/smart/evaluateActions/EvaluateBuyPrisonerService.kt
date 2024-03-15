package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.*
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import service.aiServices.smart.EvaluateBestPosition
import service.aiServices.smart.SmartAI

/**
 * Klass to simulate buy prisoner from other isolation action.
 * @property evaluateBestPosition is assigned to use getBestPositions function
 */
class EvaluateBuyPrisonerService(val smartAI: SmartAI) {
    private val evaluateBestPosition = EvaluateBestPosition(smartAI)

    /**
     * function to simulate the action.
     */
    fun getScoreBuyPrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionBuyPrisoner {
        val player = game.players[game.currentPlayer]

        /*finding the other player to access its isolation
        * This works for 2 players but not for games with more than 2 players.*/
        val otherPlayers = game.players.filter { it != player }

        var tile = otherPlayers[0].isolation.peek()
        /*For the cases where there is no place*/
        val placeCard = evaluateBestPosition.getBestPositions(tile,player)



        if (player.coins < 2 || otherPlayers[0].isolation.isEmpty() || placeCard == null) {
            return ActionBuyPrisoner(false,0,otherPlayers[0],
                PlaceCard(Pair(0,0) ,null,null,null))
        }

        otherPlayers[0].coins++
        player.coins -= 2

        tile = otherPlayers[0].isolation.pop()


        val actionFree = smartAI.minMax(game, depth, maximize, amountActions)
        otherPlayers[0].coins--
        player.coins += 2
        otherPlayers[0].isolation.push(tile)
        player.board.setPrisonYard(placeCard.placePrisoner.first,placeCard.placePrisoner.second,null)


        return ActionBuyPrisoner(actionFree.validAction, actionFree.score, player,placeCard)

    }



}
package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionMovePrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI
/**
 * Class that stores the function that is necessary
 * to simulate the action of move own prisoner.
 */
class EvaluateMoveOwnPrisonerService(private val smartAI: SmartAI) {
    /**
     * simulates the move the prisoner from the isolation to the prison yard
     * by calling forSimulatePlacePrisoner function.
     */
    fun getScoreMoveOwnPrisoner(game: AquaGhetto, depth: Int): ActionMovePrisoner {
        //return ActionMovePrisoner(false, 0, PlaceCard(Pair(0,0)))

        val player = game.players[game.currentPlayer]

        val action = this.forSimulatePlacePrisoner(game, depth, player)
            ?: return ActionMovePrisoner(false, 0, PlaceCard(Pair(0,0)))

        return action
    }

    /**
     * First the player is taken from the isolation and placed the best position by finding
     * the best position with getBestPositions function.
     *
     * bonus cases are checked at the end as well.
     *
     * Note: this action should get rarely used buying extension first is better
     */
    private fun forSimulatePlacePrisoner(game: AquaGhetto, depth: Int, player: Player): ActionMovePrisoner? {
        if (player.coins < 1 || player.isolation.isEmpty()) {
            return null
        }

        val removedTile = player.isolation.pop()
        player.coins -= 1

        /*don't do anything if no valid place*/
        val pos = smartAI.evaluateBestPosition.getBestPositions(removedTile, player, game)

        if (pos == null) {
            player.coins += 1
            player.isolation.add(removedTile)
            return null
        }

        val undoData = smartAI.simulatePlacement(pos.first, removedTile, pos.second, player)

        val nextPlayer = smartAI.getNextAndOldPlayer(game,false)
        game.currentPlayer = nextPlayer.second

        val bestAction = smartAI.minMax(game, depth)

        game.currentPlayer = nextPlayer.first

        val result = ActionMovePrisoner(true, bestAction.score, pos.first)

        player.coins += 1
        smartAI.undoSimulatePlacement(pos.first, pos.second, player, undoData)
        player.isolation.add(removedTile)

        /*buying a prisoner is useful if the player gets a baby*/
        /*in endgame useful if player earns employee or cannot buy extension or this decreases negative points from isolation*/
        val baby = pos.first.placeBonusPrisoner
        val first = pos.first.firstTileBonusEmployee
        val second = pos.first.secondTileBonusEmployee
        return if (baby != null) {
            result
        } else if ((first != null || second != null) && (game.drawStack.size + game.finalStack.size) < 40) {
            result
        } else if ((game.drawStack.size + game.finalStack.size) < 30) {
            result
        } else {
            null
        }

    }

}
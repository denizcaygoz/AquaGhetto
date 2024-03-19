package service.aiServices.smart

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import kotlin.math.round

class EvaluateGamePositionService(private val smartAI: SmartAI) {

    fun evaluateCurrentPosition(game: AquaGhetto, player: Player): Int {
        var points = 0

        /*get points from all prisoners*/
        val prisoners = smartAI.rootService.evaluationService.getPrisonerTypeCount(player)
        for (typeAmount in prisoners) {
            points += typeAmount.value * 10
        }

        /*guards*/
        for (guard in player.board.guardPosition) {
            points += smartAI.rootService.evaluationService.getExtraPointsForGuard(guard.first , guard.second, player.board) * 10
        }

        /*secretary*/
        points += player.secretaryCount * player.coins * 10

        /*lawyer*/
        for (xIterator in player.board.getPrisonYardIterator()) {
            for (yIterator in xIterator.value) {
                val tile = yIterator.value
                if (tile !is PrisonerTile) continue
                if (tile.prisonerTrait == PrisonerTrait.RICH) {
                    points += player.lawyerCount * 10
                }
            }
        }

        /*isolation*/
        val toRemovePointsPer = if (player.hasJanitor) 10 else 20
        val isolationPrisonerTypes = mutableSetOf<PrisonerType>()
        for (prisonerTile in player.isolation) {
            isolationPrisonerTypes.add(prisonerTile.prisonerType)
        }
        points -= toRemovePointsPer * isolationPrisonerTypes.size
        /*end of normal points*/

        /*special points for the AI*/

        /*bonus for coins*/
        if (game.drawStack.size > 2) {
            points += player.coins * 2
        }

        /*bonus for employee*/
        points += if (player.hasJanitor) 2 else 0
        points += player.lawyerCount * 2
        points += player.secretaryCount * 1
        points += player.board.guardPosition.size * 2

        /*negative points for isolation*/
        points -= player.isolation.size * 5

        /*bonus points for prison size*/
        points += countSpace(player.board)

        return points
    }

    private fun countSpace(board: Board): Int {
        var countSpace = 0.0
        for (xIterator in board.getPrisonGridIterator()) {
            for (yIterator in xIterator.value) {
                if (!yIterator.value) continue
                countSpace += 0.2
            }
        }
        return round(countSpace).toInt()
    }

}
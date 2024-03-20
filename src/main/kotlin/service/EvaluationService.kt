package service

import entity.Board
import entity.Player
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile

/**
 * Service layer class that provides basic functions to evaluate game actions, like evaluating the game, getting
 * the amount of points a player has, getting a map containing the amount of different prisoners a player own
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class EvaluationService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Calculates the current score of a player and updates its current score
     *
     * Calculates and sets the current score based on the following rules
     * 1 point for every prisoner on the grid
     * 1 secretary -> one point per coin, 2 secretary -> two points per coin
     * 1 lawyer -> one point for every rich prisoner, 2 lawyer -> two points for every rich prisoner
     * guard -> one point for every surrounding Prisoner, which is not old
     * isolation -> minus one point with a janitor / minus two without a prisoner for every prisoner type
     *
     * @param player the player whose score should be calculated
     * @return the score of a player
     */
    fun evaluatePlayer(player: Player): Int {
        var points = 0

        /*get points from all prisoners*/
        val prisoners = this.getPrisonerTypeCount(player)
        for (typeAmount in prisoners) {
            points += typeAmount.value
        }

        /*guards*/
        for (guard in player.board.guardPosition) {
            points += this.getExtraPointsForGuard(guard.first , guard.second, player.board)
        }

        /*secretary*/
        points += player.secretaryCount * player.coins

        /*lawyer*/
        for (xIterator in player.board.getPrisonYardIterator()) {
            for (yIterator in xIterator.value) {
                val tile = yIterator.value
                if (tile !is PrisonerTile) continue
                if (tile.prisonerTrait == PrisonerTrait.RICH) {
                    points += player.lawyerCount
                }
            }
        }

        /*isolation*/
        val toRemovePointsPer = if (player.hasJanitor) 1 else 2
        val isolationPrisonerTypes = mutableSetOf<PrisonerType>()
        for (prisonerTile in player.isolation) {
            isolationPrisonerTypes.add(prisonerTile.prisonerType)
        }
        points -= toRemovePointsPer * isolationPrisonerTypes.size

        player.currentScore =points
        return points
    }

    /**
     * Calculates the amount of bonus point a player ears from a guard
     *
     * @param x the x coordinate of the guard
     * @param y the y coordinate of the guard
     * @param board the board of the player
     * @return the extraPoints from the guard
     */
    fun getExtraPointsForGuard(x: Int, y: Int, board: Board): Int {
        var extraPoints = 0
        for (xIterator in -1..1) {
            for (yIterator in -1..1) {
                val tile = board.getPrisonYard(x + xIterator, y + yIterator)
                if (tile !is PrisonerTile || tile.prisonerTrait == PrisonerTrait.OLD) continue
                val type = tile.prisonerType
                if (type == PrisonerType.RED || type == PrisonerType.GREEN || type == PrisonerType.BLUE) {
                    extraPoints++
                }
            }
        }
        return extraPoints
    }

    /**
     * Evaluates and ends the game by calling evaluatePlayer for every player
     * and calling refreshAfterEndGame
     *
     * @throws IllegalStateException if there is no running game
     */
    fun evaluateGame() {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }
        for (player in game.players) {
            player.currentScore = this.evaluatePlayer(player)
            println("${player.name}: ${player.currentScore}")
        }
        onAllRefreshables {
            refreshAfterEndGame()
        }
    }

    /**
     * Creates a map containing the amount of different PrisonerTypes a player owns
     *
     * @param player the player whose map should be created
     * @return the map containing the amount of prisoners
     */
    fun getPrisonerTypeCount(player: Player): MutableMap<PrisonerType, Int> {
        val resultMap = mutableMapOf<PrisonerType, Int>()

        for (xIterator in player.board.getPrisonYardIterator()) {
            for (yIterator in xIterator.value) {
                val tile = yIterator.value
                if (tile !is PrisonerTile) continue
                val current = resultMap[tile.prisonerType] ?: 0
                resultMap[tile.prisonerType] = current + 1
            }
        }

        return resultMap
    }

}
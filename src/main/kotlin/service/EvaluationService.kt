package service

import entity.Player
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile

class EvaluationService(private val rootService: RootService): AbstractRefreshingService() {

    fun evaluatePlayer(player: Player): Int {
        return 0
    }

    fun evaluateGame() {

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
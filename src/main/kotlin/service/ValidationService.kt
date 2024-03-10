package service

import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile

/**
 * Service layer class that provides basic functions to validate if a player can make this action
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class ValidationService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Validates if a card is allowed to be placed at a specific location
     *
     * @param x the x-coordinate to place the tile onto
     * @param y the y-coordinate to place the tile onto
     * @param tile the tile to place
     * @return if this location is valid
     * @throws IllegalStateException if there is no running game
     */
    fun validateTilePlacement(tile: PrisonerTile, x: Int, y: Int): Boolean {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        val player = game.players[game.currentPlayer]
        val board = player.board

        /*Check if there is a grid to place the tile onto and there is no other card*/
        val isOccupied = board.getPrisonYard(x , y) != null
        val isPrisonGrid = board.getPrisonGrid(x , y)
        if (isOccupied || !isPrisonGrid) return false

        /*check if there is no other prisonerType around*/
        /*check if there is a prisoner of the same type around*/
        var surroundingSameType = false
        for (xIterate in -1..1) {
            for (yIterate in -1..1) {
                val tileCheck = board.getPrisonYard(x , y)
                if (tileCheck is PrisonerTile && (tileCheck.prisonerType != tile.prisonerType)) {
                    return false
                } else if (tileCheck is PrisonerTile && xIterate != 0 && yIterate != 0) {
                    surroundingSameType = true
                }
            }
        }

        /*player is not allowed to have two groups of the same prisoner type*/
        val prisonerTypeCount = rootService.evaluationService.getPrisonerTypeCount(player)
        val amount = prisonerTypeCount[tile.prisonerType]
        if (amount != null && amount > 0 && !surroundingSameType) return false

        /*requirements for placing a card are fulfilled*/
        return true
    }


    fun validateExpandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int): Boolean {
        return false
    }

}
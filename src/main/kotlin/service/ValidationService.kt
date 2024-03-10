package service

import entity.Board
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
                val tileCheck = board.getPrisonYard(x + xIterate, y + yIterate)
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

    /**
     * Validates if an extension is allowed to be placed at a specific location
     *
     * @param x the x-coordinate to place the extension onto
     * @param y the y-coordinate to place the extension onto
     * @param isBigExtension if this is a big or a small extension
     * @return if this location is valid
     * @throws IllegalStateException if there is no running game
     */
    fun validateExpandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int): Boolean {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        val player = game.players[game.currentPlayer]
        val board = player.board

        /*create a list of locations where a grid would be placed*/
        val placementCoordinates: MutableList<Pair<Int, Int>> = mutableListOf()
        placementCoordinates.add(Pair(x,y))

        if (isBigExtension) {
            placementCoordinates.add(Pair(x+1,y))
            placementCoordinates.add(Pair(x,y-1))
            placementCoordinates.add(Pair(x+1,y-1))
        } else {
            when(rotation) {
                0 -> {
                    placementCoordinates.add(Pair(x,y-1))
                    placementCoordinates.add(Pair(x+1,y-1))
                }
                90 -> {
                    placementCoordinates.add(Pair(x-1,y))
                    placementCoordinates.add(Pair(x-1,y-1))
                }
                180 -> {
                    placementCoordinates.add(Pair(x,y+1))
                    placementCoordinates.add(Pair(x-1,y+1))
                }
                270 -> {
                    placementCoordinates.add(Pair(x+1,y+1))
                    placementCoordinates.add(Pair(x+1,y))
                }
            }
        }

        /*
         * check if there is another grid next to the expansion grid and that the expansion grid is
         * not place on a currently existing grid
         */
        var nextTo = false
        for(location in placementCoordinates) {
            if (board.getPrisonGrid(location.first , location.second)) return false
            if (checkIfSurroundedByOtherGrid(board , location)) {
                nextTo = true
            }
        }

        /*expansion grid can only be placed next to an existing grid*/
        if (!nextTo) return false

        /*requirements for placing a card are fulfilled*/
        return true
    }

    private fun checkIfSurroundedByOtherGrid(board: Board, location: Pair<Int, Int>): Boolean {
        for (xIterate in -1..1) {
            for (yIterate in -1..1) {
                if (board.getPrisonGrid(location.first + xIterate, location.second + yIterate)) {
                    return true
                }
            }
        }
        return false
    }

}
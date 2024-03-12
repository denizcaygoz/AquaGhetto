package service

import entity.Board
import entity.Player
import entity.enums.PrisonerType
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
        val toCheckSurrounding = mutableListOf(Pair(1,0),Pair(-1,0),Pair(0,1),Pair(0,-1))
        var surroundingSameType = false
        for (offset in toCheckSurrounding) {
            val tileCheck = board.getPrisonYard(x + offset.first, y + offset.second)
            if (tileCheck is PrisonerTile && (tileCheck.prisonerType != tile.prisonerType)) {
                return false
            } else if (tileCheck is PrisonerTile && offset.first != 0 && offset.second != 0) {
                surroundingSameType = true
            }
        }

        /*player is not allowed to have two groups of the same prisoner type*/
        val prisonerTypeCount = rootService.evaluationService.getPrisonerTypeCount(player)
        val amount = prisonerTypeCount[tile.prisonerType]
        if (amount != null && amount > 0 && !surroundingSameType) return false

        /*Check if player has not reached the maximum amount of prisoner types*/
        if (!checkTypeAmount(player, prisonerTypeCount, tile.prisonerType)) return false

        /*requirements for placing a card are fulfilled*/
        return true
    }

    /**
     * Function to check if a player does not exceed the maximum amount of
     * different prisoner types he can own
     *
     * @param player the player to check
     * @param prisonerType a map containing the prisonerTypes as the key and the amount of prisoners a players
     * owns as the value
     * @param prisonerType the type a player wants to place
     * @return true if a player does not exceed the maximum amount of prisoner types
     */
    private fun checkTypeAmount(player: Player, prisonerTypeCount: MutableMap<PrisonerType, Int>,
                                prisonerType: PrisonerType): Boolean {
        var amount = 0

        /*counts the amount of different types a player owns*/
        for (prisonerIterator in prisonerTypeCount) {
            if (prisonerIterator.value > 0) amount++
        }

        /*if the player does not own a prisoner of the type, he increases his type count by one*/
        if (prisonerTypeCount[prisonerType] == 0) {
            amount++
        }

        return amount <= player.maxPrisonerTypes
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

    /**
     * checks whether there is a grid surrounding the supplied location
     *
     * @param board the board in which the check is performed
     * @param location the location of the intended new grid
     * @return if the location is surrounded by a grid
     */
    private fun checkIfSurroundedByOtherGrid(board: Board, location: Pair<Int, Int>): Boolean {
        return board.getPrisonGrid(location.first + 1, location.second + 0) ||
                board.getPrisonGrid(location.first - 1, location.second + 0) ||
                board.getPrisonGrid(location.first + 0, location.second + 1) ||
                board.getPrisonGrid(location.first + 0, location.second - 1)
    }

}
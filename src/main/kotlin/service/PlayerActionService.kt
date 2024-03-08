package service

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.PrisonBus
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import view.Refreshable

class PlayerActionService(private val rootService: RootService): Refreshable {

    fun addTileToPrisonBus(tile: Tile, prisonBus: PrisonBus) {

    }

    fun takePrisonBus(prisonBus: PrisonBus) {

    }

    fun placePrisoner(tile: PrisonerTile, x: Int, y: Int): Tile? {
        return null
    }

    fun movePrisonerToPrisonYard(x: Int, y: Int) {

    }

    /*new prisoner -> sourceX = sourceY = -101*/
    /*isolation prisoner -> sourceX = sourceY = -102*/
    fun moveEmployee(sourceX: Int, sourceY: Int , destinationX: Int, destinationY: Int) {

    }

    fun buyPrisonerFromOtherIsolation(player: Player, x: Int, y: Int) {

    }

    fun freePrisoner() {

    }

    /**
     * [expandPrisonGrid] places a prison extension on a valid (x,y) on the board.
     *
     * @throws IllegalStateException when there is no game running
     * @throws IllegalArgumentException when the player has not enough money
     * @throws IllegalArgumentException when the placement of the extension is not valid
     *
     **/
    fun expandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int) {
        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "There is no game running" }

        val currentPlayer: Player = game.players[game.currentPlayer]
        val neededMoney: Int = if(isBigExtension) 2 else 1
        val board: Board = currentPlayer.board

        require(currentPlayer.money >= neededMoney) { "The current player has not enough money" }
        require(board.getPrisonGrid(x,y)) { "The expansion can not be placed at this (x,y)" }

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

        placementCoordinates.forEach { coordinates ->
            require(board.getPrisonGrid(
                coordinates.first,
                coordinates.second)
            ) { "The expansion can not be placed at this (x,y)" }
        }

        placementCoordinates.forEach { coordinates ->
            board.setPrisonGrid(coordinates.first, coordinates.second, true)
        }

        currentPlayer.money -= neededMoney
        currentPlayer.remainingBigExtensions -= if (isBigExtension) 1 else 0
        currentPlayer.remainingSmallExtensions -= if (!isBigExtension) 1 else 0

        //rootService.gameService.determineNextPlayer()

        /**
         * onAllRefreshables { refreshPrison() }
         **/
    }

    fun checkBabyPrisoner(): PrisonerType? {
        return null
    }

}
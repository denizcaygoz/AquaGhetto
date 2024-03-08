package service

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.PrisonBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import view.Refreshable

class PlayerActionService(private val rootService: RootService): AbstractRefreshingService() {

    fun addTileToPrisonBus(tile: Tile, prisonBus: PrisonBus) {

    }

    fun takePrisonBus(prisonBus: PrisonBus) {

    }

    fun placePrisoner(tile: PrisonerTile, x: Int, y: Int): Tile? {
        return null
    }

    fun movePrisonerToPrisonYard(x: Int, y: Int) {

    }

    /* new employee -> sourceX = sourceY = -101 */
    /*isolation prisoner -> sourceX = sourceY = -102*/
    /* secretary -> sourceX = sourceY = -103 */
    /* janitor -> sourceX = sourceY = -104 */
    /* lawyer -> sourceX = sourceY = -105 */
    fun moveEmployee(sourceX: Int, sourceY: Int , destinationX: Int, destinationY: Int) {
        checkNotNull(rootService.currentGame) { "No game is running right now."}
        val game = rootService.currentGame
        val currentPlayer = game.players[game.currentPlayer]

        var employeeToMove: Tile? = null
        if (sourceX == sourceY && sourceX < -100) {
            when (sourceX) {
                -103 -> {
                    check(currentPlayer.secretaryCount > 0) { "Current player doesn't have any secretarys to move."}
                    currentPlayer.secretaryCount--
                }
                -104 -> {
                    check(currentPlayer.hasJanitor) { "Current player doesn't have a janitor to move."}
                    currentPlayer.hasJanitor = false
                }
                -105 -> {
                    check(currentPlayer.lawyerCount > 0) { "Current player doesn't have a lawyer to move."}
                    currentPlayer.lawyerCount--
                }
            }
        } else {
            employeeToMove = GuardTile()
            currentPlayer.board.guardPosition.remove(Pair(sourceX, sourceY))
        }

        if (destinationX == destinationY && destinationX < -100) {
            when (destinationX) {
                -103 -> {
                    currentPlayer.secretaryCount++
                }
                -104 -> {
                    currentPlayer.hasJanitor = true
                }
                -105 -> {
                    currentPlayer.lawyerCount++
                }
            }
        } else {
            val isOccupied = currentPlayer.board.getPrisonGrid(destinationX, destinationY)
            check(isOccupied)
            currentPlayer.board.setPrisonYard(destinationX, destinationY, employeeToMove)
            currentPlayer.board.guardPosition.add(Pair(destinationX, destinationY))
        }

        onAllRefreshables {
            refreshEmployee(currentPlayer)
            refreshScoreStats()
        }
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
        if (isBigExtension) {
            currentPlayer.remainingBigExtensions -= 1
            currentPlayer.maxPrisonerTypes += 1
        } else {
            currentPlayer.remainingSmallExtensions -= 1
        }

        //rootService.gameService.determineNextPlayer()

        /**
         * onAllRefreshables { refreshPrison() }
         **/
    }

    fun checkBabyPrisoner(): PrisonerType? {
        val game = rootService.currentGame
        checkNotNull(game) { "No game started yet." }

        /*get breedable prisoners*/
        val foundBreedableMale = mutableMapOf<PrisonerType, PrisonerTile>()
        val foundBreedableFemale = mutableMapOf<PrisonerType, PrisonerTile>()
        val player = game.players[game.currentPlayer]
        val board = player.board
        for (entry1 in board.getPrisonYardIterator()) {
            val secondMap = entry1.value
            for (entry2 in secondMap) {
                val tile = entry2.value
                if (tile !is PrisonerTile) continue
                val trait = tile.prisonerTrait
                val type = tile.prisonerType
                if (trait == PrisonerTrait.MALE || tile.breedable) foundBreedableMale[type] = tile
                if (trait == PrisonerTrait.FEMALE || tile.breedable) foundBreedableFemale[type] = tile
            }
        }

        /*check for breedable prisoners*/
        for (type in PrisonerType.values()) {
            val male: PrisonerTile? = foundBreedableMale[type]
            val female: PrisonerTile? = foundBreedableMale[type]
            if (male != null && female != null) {
                male.breedable = false
                female.breedable = false
                return type
            }
        }

        /*no breedable prisoner was found*/
        return null
    }

}
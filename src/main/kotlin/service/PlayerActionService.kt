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

/**
 * Service layer class that provides basic functions for the actions a player can take
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class PlayerActionService(private val rootService: RootService): AbstractRefreshingService() {

    fun addTileToPrisonBus(tile: Tile, prisonBus: PrisonBus) {

    }

    fun takePrisonBus(prisonBus: PrisonBus) {

    }

    fun placePrisoner(tile: PrisonerTile, x: Int, y: Int): Pair<Boolean,PrisonerTile?> {
        return Pair(false, null)
    }

    /**
     * Moves a prisoner from the player's isolation area to the game board's prison yard.
     * Requires a payment of one coin from the player.
     *
     * @param x The x-coordinate on the game board to place the prisoner.
     * @param y The y-coordinate on the game board to place the prisoner.
     *
     * @throws IllegalStateException if the game has not been started yet.
     * @throws IllegalArgumentException if the player does not have enough coins, or if the player's isolation area is empty.
     */
    fun movePrisonerToPrisonYard(x: Int, y: Int): Pair<Boolean,PrisonerTile?> {
        val game = rootService.currentGame
        checkNotNull(game) { "No game started yet." }

        val player = game.players[game.currentPlayer]

        // Ensure the player has enough coins for the move
        check(player.coins >= 1) { "Bring more money and come back!" }

        // Ensure the player's isolation area is not empty
        check(player.isolation.isNotEmpty()) { "Empty Isolation." }

        // Pop a prisoner tile from the player's isolation area
        val tile = player.isolation.pop()

        // Place the prisoner on the game board's prison yard
        val bonus = placePrisoner(tile, x, y)

        // Deduct one coin from the player
        player.coins--

        // Refresh isolation area and prison layout for all observers
        onAllRefreshables {
            refreshIsolation(player)
            refreshPrison(tile, x, y)
        }

        return bonus
    }



    /* new employee -> sourceX = sourceY = -101 */
    /*isolation prisoner -> sourceX = sourceY = -102    <- isolation is janitor*/
    /* secretary -> sourceX = sourceY = -103 */
    /* janitor -> sourceX = sourceY = -104 */
    /* lawyer -> sourceX = sourceY = -105 */
    fun moveEmployee(sourceX: Int, sourceY: Int , destinationX: Int, destinationY: Int) {
        val game = rootService.currentGame

        checkNotNull(game) { "No game is running right now."}

        val currentPlayer = game.players[game.currentPlayer]
        var employeeToMove: Tile? = null
        var hasSetJanitorHere = false

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
            val sourceTile = currentPlayer.board.getPrisonYard(sourceX, sourceY)
            checkNotNull(sourceTile) { "There is no tile at that position."}
            employeeToMove = GuardTile()
            currentPlayer.board.setPrisonYard(sourceX, sourceY, null)
            currentPlayer.board.guardPosition.remove(Pair(sourceX, sourceY))
        }

        if (destinationX == destinationY && destinationX < -100) {
            when (destinationX) {
                -103 -> {
                    check(currentPlayer.secretaryCount < 2) { "Current player has the maximum amount of secretaries."}
                    currentPlayer.secretaryCount++
                }
                -104 -> {
                    check(!currentPlayer.hasJanitor) { "Current player already has a Janitor."}
                    currentPlayer.hasJanitor = true
                    hasSetJanitorHere = true
                }
                -105 -> {
                    check(currentPlayer.secretaryCount < 2) { "Current player has the maximum amount of lawyers."}
                    currentPlayer.lawyerCount++
                }
            }
        } else {
            val destinationTile = currentPlayer.board.getPrisonYard(destinationX, destinationY)
            check(destinationTile == null) { "Another tile already occupies this position."}
            currentPlayer.board.setPrisonYard(destinationX, destinationY, employeeToMove)
            currentPlayer.board.guardPosition.add(Pair(destinationX, destinationY))
        }

        onAllRefreshables {
            refreshEmployee(currentPlayer) // aktualisiert refreshEmployee auch die GuardTiles?
            if (hasSetJanitorHere) {
                refreshIsolation(currentPlayer)
            }
            refreshScoreStats()
        }
    }

    fun buyPrisonerFromOtherIsolation(player: Player, x: Int, y: Int): Pair<Boolean,PrisonerTile?> {
        val game = rootService.currentGame

        checkNotNull(game) { "No game is currently running."}

        val currentPlayer = game.players[game.currentPlayer]

        check(currentPlayer.coins >= 2) { "Insufficient player funds."}
        check(player.isolation.isNotEmpty()) { "Player has no Prisoner in their isolation." }
        check(player != currentPlayer) { "Player can't buy from their own isolation."}

        // Transferring money
        player.coins++
        currentPlayer.coins -= 2

        // Fetching the Prisoner from the selected player
        val prisonerFromSelectedPlayersIsolation = player.isolation.pop()
        val bonusTile = placePrisoner(prisonerFromSelectedPlayersIsolation, x, y)

        onAllRefreshables {
            refreshScoreStats()
            refreshIsolation(player)
            refreshPrison(prisonerFromSelectedPlayersIsolation, x, y)
        }

        return bonusTile
    }

    /**
     * [freePrisoner] discards the tile on top of the isolation stack.
     *
     * @throws IllegalStateException when there is no game running
     * @throws IllegalArgumentException when the player has not enough money
     * @throws IllegalArgumentException when there is no prisoner on his isolation stack
     *
     **/
    fun freePrisoner() {
        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "There is no game running" }

        val currentPlayer: Player = game.players[game.currentPlayer]
        require(currentPlayer.coins >= 2) { "The current player has not enough money" }
        require(!currentPlayer.isolation.empty()) { "There is not prisoner to be freed" }

        currentPlayer.isolation.pop()
        currentPlayer.coins -= 2

        rootService.evaluationService.evaluatePlayer(currentPlayer)

        //rootService.gameService.determineNextPlayer()

        /**
         * onAllRefreshables { refreshIsolation() }
         * */
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

        require(currentPlayer.coins >= neededMoney) { "The current player has not enough money" }
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

        currentPlayer.coins -= neededMoney
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
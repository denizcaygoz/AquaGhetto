package service

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.PrisonBus
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.networkService.ConnectionState


/**
 * Service layer class that provides basic functions for the actions a player can take
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class PlayerActionService(private val rootService: RootService): AbstractRefreshingService() {

    fun addTileToPrisonBus(tile: Tile, prisonBus: PrisonBus, sender: PlayerType = PlayerType.PLAYER) {
        val isNetworkGame = rootService.networkService.connectionState != ConnectionState.DISCONNECTED
        val game = rootService.currentGame
        checkNotNull(game) { "No game started yet." }

        val currentPlayer = game.players.getOrNull(game.currentPlayer)
        checkNotNull(currentPlayer) { "Invalid current player." }

        // To check if the game has at least one prison bus available
        require(game.prisonBuses.isNotEmpty()) {"No prison buses available."}

        // To check if the current player has not taken a bus yet
        require(currentPlayer.takenBus == null) {"Current player has already taken a bus."}

        // To check tile is not an instance of GuardTile
        require(tile !is GuardTile) {"Cannot add a guard tile to a prison bus."}

        // To check if there is at least one slot that is empty and not blocked
        var emptyAndUnblockedIndex = -1
        for (index in prisonBus.tiles.indices) {
            if (prisonBus.tiles[index] == null && !prisonBus.blockedSlots[index]) {
                emptyAndUnblockedIndex = index
                break
            }
        }
        require(emptyAndUnblockedIndex != -1) { "No empty slots available on the bus." }

        // Add the tile to the prison bus
        prisonBus.tiles[emptyAndUnblockedIndex] = tile

        if (isNetworkGame && sender == PlayerType.PLAYER) {
            rootService.networkService.sendAddTileToTruck(prisonBus)
            // Nur weil determineNextPlayer nicht implementiert wurde
            rootService.networkService.updateConnectionState(ConnectionState.WAITING_FOR_TURN)
        }

        rootService.gameService.determineNextPlayer(false)

        onAllRefreshables {
            refreshPrisonBus(prisonBus)
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    fun takePrisonBus(prisonBus: PrisonBus) {
        val game = rootService.currentGame ?: throw IllegalStateException("No game started yet.")

        val currentPlayer = game.players.getOrNull(game.currentPlayer)
            ?: throw IllegalStateException("Invalid current player.")

        // To check if the current player has not taken a bus yet
        require(currentPlayer.takenBus == null) { "Current player has already taken a bus." }

        // To check if the game has at least one prison bus available
        require(game.prisonBuses.isNotEmpty()) { "No prison buses available." }

        // To check if the prisonBus has at least one card on it
        require(prisonBus.tiles.any { it != null }) { "Selected prison bus has no cards." }

        currentPlayer.takenBus = prisonBus
        game.prisonBuses.remove(prisonBus)

        var hasBusCoinTile = false
        //check if one of tiles in prison bus is an instance of CoinTile
        //then increment currentPlayer.money by 1 and remove the tile from prisonBus.tiles
        prisonBus.tiles.forEachIndexed { index, tile ->
            if (tile is CoinTile) {
                currentPlayer.coins += 1
                prisonBus.tiles[index] = null
                hasBusCoinTile = true
            }

            onAllRefreshables {
                if (hasBusCoinTile) {
                    refreshScoreStats()
                }
                refreshPrisonBus(prisonBus)
            }
        }

        rootService.gameService.determineNextPlayer(true)

        onAllRefreshables {
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    /**
     * Places a [PrisonerTile] on the game board at the specified coordinates and evaluates scoring conditions.
     * A [PrisonerTile] is placed in the isolation if (-100, -100) is passed as coordinate.
     *
     * @param tile The [PrisonerTile] to be placed.
     * @param x The x-coordinate on the game board.
     * @param y The y-coordinate on the game board.
     * @return A Pair indicating the success of the placement and a [PrisonerTile] with a [PrisonerTrait.BABY], if applicable.
     *   - First value (Boolean): True if the placement is successful, false otherwise.
     *   - Second value (PrisonerTile?): A [PrisonerTile] with [PrisonerTrait.BABY] if a baby can be conceived,
     *   otherwise null.
     *
     * @throws IllegalStateException if the game has not been started yet.
     */
    fun placePrisoner(tile: PrisonerTile, x: Int, y: Int): Pair<Boolean, PrisonerTile?> {
        val game = rootService.currentGame
        checkNotNull(game) { "No game started yet." }
        val player = game.players[game.currentPlayer]
        val board = player.board

        val result: Pair<Boolean, PrisonerTile?>

        // Validate the tile placement
        if (rootService.validationService.validateTilePlacement(tile, x, y)) {
            // Set the PrisonerTile on the game board
            board.setPrisonYard(x, y, tile)

            // Calculate the count of the specified PrisonerType in the player's PrisonYard
            val count = rootService.evaluationService.getPrisonerTypeCount(player).get(tile.prisonerType)!!
            // Never null because of setPrisonYard

            // Evaluate scoring conditions based on the count
            if (count % 3 == 0 && count != 0) {
                // Increment player's coins for every third tile, excluding counts of 0
                player.coins++
            }
            if (count % 5 == 0 && count != 0) {
                // Place a GuardTile at (-101, -101) and return it for every fifth tile, excluding count of 0
                board.setPrisonYard(-101, -101, GuardTile())
            }
            // Checking for baby prisoner
            result = Pair(true,
                rootService.playerActionService.checkBabyPrisoner()
                    ?.let { rootService.boardService.getBabyTile(it) })

            // Refresh score statistics and the prison layout
            onAllRefreshables {
                refreshScoreStats()
                refreshPrison(tile, x, y)
            }

            if (result.second != null) { // No baby tile, turn is over
                rootService.gameService.determineNextPlayer(false)
                onAllRefreshables { refreshAfterNextTurn(game.players[game.currentPlayer]) }
            }

            return result

        } else if (x == y && x == -100) { // To put prisoners into a player's isolation
            player.isolation.push(tile)
            result = Pair(true, null)

            rootService.gameService.determineNextPlayer(false)

            onAllRefreshables {
                refreshIsolation(player)
                refreshScoreStats()
                refreshAfterNextTurn(game.players[game.currentPlayer])
            }

            return result
        }


        // Return indicating unsuccessful placement
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

        // Deduct one coin from the player
        player.coins--

        // Refresh isolation area and prison layout for all observers
        onAllRefreshables {
            refreshIsolation(player)
        }

        // Place the prisoner on the game board's prison yard
        return placePrisoner(tile, x, y)
    }


    /**
     * Places a new employee or moves an employee from a source location to another destination.
     * To determine if a janitor, secretary or lawyer must be moved/placed, special coordinates
     * are used as an indicator:
     *
     * - new employee without a role: (-101, -101)
     * (Note: only be used as ([sourceX], [sourceY])
     * - janitor: (-102, -102)
     * - secretary: (-103, -103)
     * - lawyer: (-104, -104)
     *
     * All other coordinates result in a guard being placed/moved.
     *
     * @param sourceX X-coordinate of the employee to be moved
     * @param sourceY Y-coordinate of the employee to be moved
     * @param sourceY Y-coordinate of the destination
     * @param sourceY Y-coordinate of the employee to be moved
     *
     * @throws IllegalStateException There is no game running
     * @throws IllegalStateException The selected player does not have enough coins.
     * @throws IllegalStateException Guards are moved from/to invalid locations
     * or the player already has the maximum number of employees from a type.
     */
    fun moveEmployee(
        sourceX: Int,
        sourceY: Int,
        destinationX: Int,
        destinationY: Int,
        sender: PlayerType = PlayerType.PLAYER
    ) {
        val isNetworkGame = rootService.networkService.connectionState != ConnectionState.DISCONNECTED
        val game = rootService.currentGame

        checkNotNull(game) { "No game is running right now."}

        val currentPlayer = game.players[game.currentPlayer]
        check(currentPlayer.coins >= 1)
        val employeeToMove: Tile = GuardTile()
        var hasSetJanitorHere = false

        if (sourceX == sourceY && sourceX < -100) {
            when (sourceX) {
                -102 -> {
                    check(currentPlayer.hasJanitor) { "Current player doesn't have a janitor to move."}
                    currentPlayer.hasJanitor = false
                    currentPlayer.coins--
                }
                -103 -> {
                    check(currentPlayer.secretaryCount > 0) { "Current player doesn't have any secretarys to move."}
                    currentPlayer.secretaryCount--
                    currentPlayer.coins--
                }
                -104 -> {
                    check(currentPlayer.lawyerCount > 0) { "Current player doesn't have a lawyer to move."}
                    currentPlayer.lawyerCount--
                    currentPlayer.coins--
                }
            }
        } else {
            val sourceTile = currentPlayer.board.getPrisonYard(sourceX, sourceY)
            checkNotNull(sourceTile) { "There is no tile at that position."}
            currentPlayer.board.setPrisonYard(sourceX, sourceY, null)
            currentPlayer.board.guardPosition.remove(Pair(sourceX, sourceY))
            currentPlayer.coins--
        }

        if (destinationX == destinationY && destinationX < -100) {
            when (destinationX) {
                -102 -> {
                    check(!currentPlayer.hasJanitor) { "Current player already has a Janitor."}
                    currentPlayer.hasJanitor = true
                    hasSetJanitorHere = true
                }
                -103 -> {
                    check(currentPlayer.secretaryCount < 2) { "Current player has the maximum amount of secretaries."}
                    currentPlayer.secretaryCount++
                }
                -104 -> {
                    check(currentPlayer.secretaryCount < 2) { "Current player has the maximum amount of lawyers."}
                    currentPlayer.lawyerCount++
                }
            }
        } else {
            val destinationTile = currentPlayer.board.getPrisonYard(destinationX, destinationY)
            val isGrid = currentPlayer.board.getPrisonGrid(destinationX, destinationY)
            check(destinationTile == null) { "Another tile already occupies this position."}
            check(isGrid) { "There is no prison tile to place the guard on."}
            currentPlayer.board.setPrisonYard(destinationX, destinationY, employeeToMove)
            currentPlayer.board.guardPosition.add(Pair(destinationX, destinationY))
        }

        rootService.gameService.determineNextPlayer(false)

        if (isNetworkGame && sender == PlayerType.PLAYER) {
            if (sourceX == sourceY && sourceX != -101) {
                rootService.networkService.sendPlaceWorker(
                    sourceX,
                    sourceY,
                    destinationX,
                    destinationY
                )
            }
        }

        onAllRefreshables {
            refreshEmployee(currentPlayer) // aktualisiert refreshEmployee auch die GuardTiles?
            if (hasSetJanitorHere) {
                refreshIsolation(currentPlayer)
            }
            refreshScoreStats()
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    /**
     * Buys a prisoner from another players isolation, if the player has sufficient funds.
     *
     * @throws IllegalStateException No game is currently live.
     * @throws IllegalStateException The current player has less than two coins.
     * @throws IllegalStateException The selected player has no prisoner in their isolation.
     * @throws IllegalStateException The selected player is the same as the current player.
     */
    fun buyPrisonerFromOtherIsolation(player: Player, x: Int, y: Int): Pair<Boolean,PrisonerTile?> {
        val game = rootService.currentGame

        checkNotNull(game) { "No game is currently running."}

        val currentPlayer = game.players[game.currentPlayer]

        check(currentPlayer.coins >= 2) { "Insufficient player funds."}
        check(player.isolation.isNotEmpty()) { "Player ${player.name} has no Prisoner in their isolation." }
        check(player != currentPlayer) { "Player can't buy from their own isolation."}

        // Transferring money
        player.coins++
        currentPlayer.coins -= 2

        // Fetching the Prisoner from the selected player
        val prisonerFromSelectedPlayersIsolation = player.isolation.pop()

        onAllRefreshables {
            refreshScoreStats()
            refreshIsolation(player)
        }

        return placePrisoner(prisonerFromSelectedPlayersIsolation, x, y)
    }

    /**
     * [freePrisoner] discards the tile on top of the isolation stack.
     *
     * @throws IllegalStateException when there is no game running
     * @throws IllegalArgumentException when the player has not enough money
     * @throws IllegalArgumentException when there is no prisoner on his isolation stack
     *
     **/
    fun freePrisoner(sender: PlayerType = PlayerType.PLAYER) {
        val isNetworkGame = rootService.networkService.connectionState != ConnectionState.DISCONNECTED
        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "There is no game running" }

        val currentPlayer: Player = game.players[game.currentPlayer]
        require(currentPlayer.coins >= 2) { "The current player: ${currentPlayer.name} has not enough money" }
        require(!currentPlayer.isolation.empty()) { "There is not prisoner to be freed" }

        currentPlayer.isolation.pop()
        currentPlayer.coins -= 2

        rootService.evaluationService.evaluatePlayer(currentPlayer)

        rootService.gameService.determineNextPlayer(false)

        if (isNetworkGame && sender == PlayerType.PLAYER) {
            rootService.networkService.sendDiscard()
            // Nur weil determineNextPlayer nicht implementiert wurde
            rootService.networkService.updateConnectionState(ConnectionState.WAITING_FOR_TURN)
        }

        onAllRefreshables {
            refreshIsolation(currentPlayer)
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    /**
     * [expandPrisonGrid] places a prison extension on a valid (x,y) on the board.
     *
     * @throws IllegalStateException when there is no game running
     * @throws IllegalArgumentException when the player has not enough money
     * @throws IllegalArgumentException when the placement of the extension is not valid
     *
     **/
    fun expandPrisonGrid(
        isBigExtension: Boolean,
        x: Int,
        y: Int ,
        rotation: Int,
        sender: PlayerType = PlayerType.PLAYER
    ) {
        val isNetworkGame = rootService.networkService.connectionState != ConnectionState.DISCONNECTED
        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "There is no game running" }

        val currentPlayer: Player = game.players[game.currentPlayer]
        val neededMoney: Int = if(isBigExtension) 2 else 1
        val board: Board = currentPlayer.board

        check(currentPlayer.coins >= neededMoney) { "The current player has not enough money" }
        check(rootService.validationService.validateExpandPrisonGrid(isBigExtension, x, y, rotation)) {
            "Cannot place extension at ($x, $y)"
        }

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
            board.setPrisonGrid(coordinates.first, coordinates.second, true)
        }

        currentPlayer.coins -= neededMoney
        if (isBigExtension) {
            currentPlayer.remainingBigExtensions -= 1
            currentPlayer.maxPrisonerTypes += 1
        } else {
            currentPlayer.remainingSmallExtensions -= 1
        }

        if (isNetworkGame && sender == PlayerType.PLAYER) {
            rootService.networkService.sendBuyExpansion(isBigExtension, x, y, rotation)
            // Nur weil determineNextPlayer nicht implementiert wurde
            rootService.networkService.updateConnectionState(ConnectionState.WAITING_FOR_TURN)
        }

        rootService.gameService.determineNextPlayer(false)

        onAllRefreshables {
            refreshPrison(null, -1, -1)
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    /**
     * Function to determine if the current player should get a bonus tile, because a baby
     * was born, this function sets breedable to false for the two tiles
     *
     * @return the type of the baby or null
     */
    private fun checkBabyPrisoner(): PrisonerType? {
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
                if (trait == PrisonerTrait.MALE && tile.breedable) foundBreedableMale[type] = tile
                if (trait == PrisonerTrait.FEMALE && tile.breedable) foundBreedableFemale[type] = tile
            }
        }

        /*check for breedable prisoners*/
        for (type in PrisonerType.values()) {
            val male: PrisonerTile? = foundBreedableMale[type]
            val female: PrisonerTile? = foundBreedableFemale[type]
            if (male != null && female != null) {
                male.breedable = false
                female.breedable = false
                return type
            }
        }

        /*no breedable prisoner was found*/
        return null
    }

    /**
     * Draws a card from the drawStack or, if empty, from the finalStack.
     *
     * @throws IllegalArgumentException No game is currently running.
     */
    fun drawCard(): Tile {
        val game = rootService.currentGame
        checkNotNull(game) { "No game is currently running." }
        val stackToDrawFrom = game.drawStack.ifEmpty { game.finalStack }
        return stackToDrawFrom.removeAt(0)
    }

}
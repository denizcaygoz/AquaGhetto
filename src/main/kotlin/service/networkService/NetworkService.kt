package service.networkService

import edu.udo.cs.sopra.ntf.*
import entity.Player
import entity.PrisonBus
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.AbstractRefreshingService
import service.RootService
import entity.AquaGhetto
import entity.enums.PlayerType
import java.util.*
import kotlin.random.Random

/**
 * Service layer class that realizes the necessary logic for sending and receiving messages
 * in multiplayer network games. Bridges between the [AqueghettoNetworkClient] and the other services.
 */
class NetworkService(private val rootService: RootService): AbstractRefreshingService() {

    companion object {
        /** URL of the BGW net server hosted for SoPra participants */
        const val SERVER_ADDRESS = "sopra.cs.tu-dortmund.de:80/bgw-net/connect"

        /** Name of the game as registered with the server */
        const val GAME_ID = "Aquaretto"
    }

    /** Network client. Nullable for offline games. */
    var client: AqueghettoNetworkClient? = null
        private set

    /**
     * current state of the connection in a network game.
     */
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    /**
     * lists to hold elements that need to be sent when taking a bus
     **/
    val prisoners: MutableList<Triple<Int, Int, Int>> = mutableListOf()
    val children: MutableList<Triple<Int, Int, PrisonerTile>> = mutableListOf()
    val workers: MutableList<Pair<Int, Int>> = mutableListOf()

    /**
     * list of players that joined the game and the name of host
     **/
    val playersJoined: MutableList<String> = mutableListOf()
    var hostPlayer: String? = null
    var createadSessionID: String? = null
    var servicePlayer: String = ""
    var serviceType: PlayerType = PlayerType.PLAYER

    /**
     * Connects to server, sets the [NetworkService.client] if successful and returns `true` on success.
     *
     * @param secret Network secret. Must not be blank (i.e. empty or only whitespaces)
     * @param name Player name. Must not be blank
     *
     * @throws IllegalArgumentException if secret or name is blank
     * @throws IllegalStateException if already connected to another game
     */
    private fun connect(secret: String, name: String): Boolean {
        require(connectionState == ConnectionState.DISCONNECTED && client == null)
        { "already connected to another game" }

        require(secret.isNotBlank()) { "server secret must be given" }
        require(name.isNotBlank()) { "player name must be given" }

        val newClient =
            AqueghettoNetworkClient(
                playerName = name,
                host = SERVER_ADDRESS,
                secret = secret,
                networkService = this
            )

        return if (newClient.connect()) {
            this.client = newClient
            true
        } else {
            false
        }
    }


    /**
     * Disconnects the [client] from the server, nulls it and updates the
     * [connectionState] to [ConnectionState.DISCONNECTED]. Can safely be called
     * even if no connection is currently active.
     */
    fun disconnect() {
        client?.apply {
            if (sessionID != null) leaveGame("Goodbye!")
            if (isOpen) disconnect()
        }
        client = null
        updateConnectionState(ConnectionState.DISCONNECTED)
    }


    /**
     * set up the game using [GameService.startNewGame] and send the game init message
     * to the guest player. [connectionState] needs to be [ConnectionState.WAITING_FOR_GUEST].
     * This method should be called from the [AqueghettoNetworkClient] when the guest joined notification
     * arrived. See [AqueghettoNetworkClient.onPlayerJoined].
     *
     * @param playerType type of player that is creating the game
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_GUEST]
     * @throws IllegalStateException if host player does not exist
     * @throws IllegalStateException if game does not exist
     * @throws IllegalStateException if draw stack were not created
     */
    fun startNewHostedGame(playerType: PlayerType = PlayerType.PLAYER) {
        check(connectionState == ConnectionState.WAITING_FOR_GUEST)
        { "currently not prepared to start a new hosted game." }

        val players: MutableList<Pair<String, PlayerType>> = mutableListOf()
        val messagePlayerList: MutableList<String> = mutableListOf()
        val drawStackList: MutableList<Int> = mutableListOf()
        val hostPlayerName: String? = hostPlayer

        checkNotNull(hostPlayerName)

        /**Add hostPlayer to playersList and to messagePlayerList **/
        players.add(Pair(hostPlayerName, playerType))
        messagePlayerList.add(hostPlayerName)

        /**Add guestPlayers to players and to messagePlayerList **/
        playersJoined.forEach { playerName ->
            players.add(Pair(playerName, PlayerType.NETWORK))
            messagePlayerList.add(playerName)
        }

        rootService.gameService.startNewGame(players)

        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "game should not be null right after starting it." }
        check(game.drawStack.size > 0) { "the game was not initialized properly." }
        check(game.finalStack.size > 0) { "the game was not initialized properly." }

        /**Combine both stack to one**/
        val drawStack: Stack<Tile> = Stack()
        drawStack.addAll(game.drawStack)
        drawStack.addAll(game.finalStack)

        drawStack.forEach { tile -> drawStackList.add(tile.id) }

        val message = InitGameMessage(
            messagePlayerList.toList(),
            drawStackList.toList()
        )

        determineNextPlayer()

        client?.sendGameActionMessage(message)

        rootService.gameService.checkAITurn(game.players[game.currentPlayer])
    }

    /**
     * Initializes the entity structure with the data given by the [InitGameMessage] sent by the host.
     * [connectionState] needs to be [ConnectionState.WAITING_FOR_INIT].
     * This method should be called from the [AqueghettoNetworkClient] when the host sends the init message.
     * See [AqueghettoNetworkClient.onInitReceived].
     *
     * @param message the [InitGameMasse] received
     * @param sender the name of the player who has joined another game
     *
     * @throws IllegalStateException if not currently waiting for an init message
     */
    fun startNewJoinedGame(message: InitGameMessage, sender: String) {
        check(connectionState == ConnectionState.WAITING_FOR_INIT)
        { "not waiting for game init message." }

        val players: MutableList<String> = mutableListOf()
        message.players.map {
            players.add(it)
        }

        val game = AquaGhetto()
        /*Create list of players*/
        val playerList = mutableListOf<Player>()
        val finalStackIndex: Int = message.drawPile.size-15
        val drawStack: Stack<Tile> = Stack()
        val finalStack: Stack<Tile> = Stack()

        servicePlayer = sender
        rootService.currentGame = game

        players.forEach { playerName ->
            val player = if(playerName == sender) {
                Player(playerName, serviceType)
            } else {
                Player(playerName, PlayerType.NETWORK)
            }
            playerList.add(player)

            rootService.gameService.initializeBoard(player.board)
        }

        game.players = playerList
        rootService.boardService.createAllTiles()

        val currentPlayer = game.players[game.currentPlayer]

        message.drawPile.forEachIndexed { index, tileId ->
            if (index >= finalStackIndex) {
               finalStack.add(game.allTiles.filter { it.id == tileId }[0])
            } else {
                drawStack.add(game.allTiles.filter { it.id == tileId }[0])
            }
        }
        game.drawStack = drawStack
        game.finalStack = finalStack

        /*Create prisonBusses*/
        game.prisonBuses = rootService.boardService.createPrisonBuses(playerList.size)

        determineNextPlayer()

        onAllRefreshables {
            refreshAfterStartGame()
            refreshAfterNextTurn(currentPlayer)
            refreshPrisonBus(null)
        }

        rootService.gameService.checkAITurn(currentPlayer)
    }

    /**
     * Connects to server and joins a game session as guest player.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the joined session (as defined by host on create)
     * @param playerType type of player that joins the game
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun joinGame(secret: String, name: String, sessionID: String, playerType: PlayerType = PlayerType.PLAYER) {
        if (!connect(secret, name)) {
            error("Connection failed")
        }
        updateConnectionState(ConnectionState.CONNECTED)

        client?.joinGame(sessionID, "Hello!")

        serviceType = playerType

        updateConnectionState(ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
    }
    /**
     * creates a session id for the current game.
     *
     * @return the sessionId
     **/
    fun createSessionID(): String {
        return "aquaghetto"+ Random.nextInt(1000)
    }

    /**
     * Connects to server and creates a new game session.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the hosted session (to be used by guest on join)
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun hostGame(secret: String, name: String, sessionID: String?) {
        if (!connect(secret, name)) {
            error("Connection failed")
        }
        updateConnectionState(ConnectionState.CONNECTED)

        createadSessionID = createSessionID()
        val newSessionID: String = createadSessionID as String

        if (sessionID.isNullOrBlank()) {
            client?.createGame(GAME_ID, newSessionID,"Welcome!", )
        } else {
            client?.createGame(GAME_ID, sessionID, "Welcome!")
        }

        hostPlayer = name
        updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }

    /**
     * send a [AddTileToTruckMessage] to the opponent
     *
     * @param prisonBus is the prison bus where the tile is being placed
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendAddTileToTruck(prisonBus: Int) {
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }

        val message = AddTileToTruckMessage(prisonBus)
        client?.sendGameActionMessage(message)

        determineNextPlayer()
    }

    /**
     * play the opponent's turn by handling the [AddTileToTruckMessage] sent through the server.
     *
     * @param message the message to handle
     *
     * @throws IllegalStateException if not currently expecting an opponent's turn
     * @throws IllegalStateException if there is no game running
     * @throws IllegalStateException if bus id is out of range
     */
    fun receiveAddTileToTruck(message: AddTileToTruckMessage, ) {
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        val game = rootService.currentGame
        val busId: Int = message.truckId

        checkNotNull(game) { "somehow the current game doesnt exist." }
        check(busId in 0 until  game.prisonBuses.size) { "there is no bus at this index" }

        val tileToAdd: Tile = rootService.playerActionService.drawCard()
        val prisonBus: PrisonBus? = game.prisonBuses.find { it.index == busId  }

        checkNotNull(prisonBus) { "prison bus does not exist" }

        rootService.playerActionService.addTileToPrisonBus(tileToAdd, prisonBus, PlayerType.NETWORK)

        determineNextPlayer()
    }

    /**
     * send a [TakeTruckMessage] to the opponent
     *
     * @param prisonBus is the prison bus that is being taken
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendTakeTruck(prisonBus: Int) {
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }
        /** get the id from given bus **/

        val listsToSend = prepareLists()
        /**create TakeTruckMessage **/
        val message = TakeTruckMessage(
            prisonBus,
            listsToSend.first. toList(),
            listsToSend.second.toList(),
            listsToSend.third.toList()
        )
        /**send message **/
        client?.sendGameActionMessage(message)
        resetLists()

        determineNextPlayer()
    }

    /**
     * play the opponent's turn by handling the [TakeTruckMessage] sent through the server.
     * placing all prisoners, children and workers on the given coordinates.
     *
     * @param message the message to handle
     *
     * @throws IllegalStateException if not currently expecting an opponent's turn
     * @throws IllegalStateException if there is no game running
     * @throws IllegalStateException if bus id is out of range
     **/
    fun receiveTakeTruck(message: TakeTruckMessage) {
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        val game = rootService.currentGame
        val busId: Int = message.truckId

        checkNotNull(game) { "somehow the current game doesnt exist." }
        check(busId in 0 until  game.prisonBuses.size) { "there is no bus at this index" }

        val currentPlayer: Player = game.players[game.currentPlayer]
        val prisonBus: PrisonBus? = game.prisonBuses.find { it.index == busId  }
        val possibleChildren: MutableList<Pair<Boolean, PrisonerTile?>> = mutableListOf()

        checkNotNull(prisonBus) { "The Prison bus does not exist" }

        rootService.playerActionService.takePrisonBus(prisonBus)

        val takenBus: PrisonBus? = currentPlayer.takenBus

        requireNotNull(takenBus) { "the player has a wrong bus" }
        require(currentPlayer.takenBus == prisonBus) { "the player has a wrong bus" }

        /** handle tiles on truck and place them **/
        message.animalList.forEach {animals ->
            val tileToPlace: Int? = takenBus.tiles[animals.truck]?.id

            require(!takenBus.blockedSlots[animals.truck]) { "this slot is blocked" }
            requireNotNull(tileToPlace) { "tile does not exist" }

            val tile = game.allTiles.filter { it.id == tileToPlace }[0]
            val child: Pair<Boolean, PrisonerTile?>
            if (tile is PrisonerTile){
                if (animals.x == 0 && animals.y == 0) {
                    child = rootService.playerActionService.placePrisoner(tile, -100, -100)
                    possibleChildren.add(child)
                } else {
                    child = rootService.playerActionService.placePrisoner(tile, animals.x, animals.y)
                    possibleChildren.add(child)
                }
            }
        }

        placeChildren(message.offspringList, possibleChildren)
        placeWorker(message.workerList)

        rootService.gameService.determineNextPlayer(true)

        determineNextPlayer()
    }

    /**
     * send a [BuyExpansionMessage] to the opponent.
     * when rotation is 0 it is the top left corner that is being sent.
     *
     * @param isBigExpansion is true when big expansion else false
     * @param x is the x coordinate of expansion
     * @param y is the y coordinate of expansion
     * @param rotation is degree of the rotation
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendBuyExpansion(isBigExpansion: Boolean, x: Int, y: Int, rotation: Int) {
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }

        val placementCoordinates: MutableList<PositionPair> = mutableListOf()
        placementCoordinates.add(PositionPair(x,y))

        if (isBigExpansion) {
            placementCoordinates.add(PositionPair(x+1,y))
            placementCoordinates.add(PositionPair(x,y-1))
            placementCoordinates.add(PositionPair(x+1,y-1))
        } else {
            when(rotation) {
                0 -> {
                    placementCoordinates.add(PositionPair(x,y-1))
                    placementCoordinates.add(PositionPair(x+1,y-1))
                }
                90 -> {
                    placementCoordinates.add(PositionPair(x-1,y))
                    placementCoordinates.add(PositionPair(x-1,y-1))
                }
                180 -> {
                    placementCoordinates.add(PositionPair(x,y+1))
                    placementCoordinates.add(PositionPair(x-1,y+1))
                }
                270 -> {
                    placementCoordinates.add(PositionPair(x+1,y+1))
                    placementCoordinates.add(PositionPair(x+1,y))
                }
            }

        }

        /**create TakeTruckMessage **/
        val message = BuyExpansionMessage(
            placementCoordinates.toList()
        )
        /**send message **/
        client?.sendGameActionMessage(message)

        determineNextPlayer()
    }

    /**
     * play the opponent's turn by handling the [BuyExpansionMessage] sent through the server.
     * placing the expansion on given coordinates.
     *
     * @param message the message to handle
     *
     * @throws IllegalStateException if not currently expecting an opponent's turn
     * @throws IllegalStateException if there is no game running
     * @throws IllegalArgumentException if the list has the wrong size
     **/
    fun receiveBuyExpansion(message: BuyExpansionMessage) {
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }

        val positions: MutableList<PositionPair> = message.positionList.toMutableList()

        require(positions.size in  1 .. 4) { "there is a wrong amount in this list" }

        val highest: Int = positions.maxOf { it.x + it.y }
        val lowest: Int = positions.minOf { it.x + it.y }
        val highestCandidates: List<PositionPair> = positions.filter { it.x + it.y == highest }
        val lowestCandidates: List<PositionPair> = positions.filter { it.x + it.y == lowest }

        if(message.positionList.size == 4) {
            val point = highestCandidates[0]
            rootService.playerActionService.expandPrisonGrid(
                true, point.x-1, point.y, 0, PlayerType.NETWORK
            )
        } else {
            when{
                (highestCandidates.size == 2 && lowestCandidates.size == 1) -> {
                    val point = lowestCandidates[0]
                    rootService.playerActionService.expandPrisonGrid(
                        false, point.x, point.y+1, 0, PlayerType.NETWORK
                    )
                }
                (highest-lowest == 2) -> {
                    positions.remove(lowestCandidates[0])
                    positions.remove(highestCandidates[0])
                    val point: PositionPair
                    val rotation: Int
                    if (positions[0].x < highestCandidates[0].x) {
                        point = highestCandidates[0]
                        rotation = 90

                    } else {
                        point = lowestCandidates[0]
                        rotation = 270
                    }

                    rootService.playerActionService.expandPrisonGrid(
                        false, point.x, point.y, rotation, PlayerType.NETWORK
                    )
                }
                (highestCandidates.size == 1 && lowestCandidates.size == 2) -> {
                    val point = highestCandidates[0]
                    rootService.playerActionService.expandPrisonGrid(
                        false, point.x, point.y-1, 180, PlayerType.NETWORK
                    )
                }
            }
        }

        determineNextPlayer()
    }

    /**
     * send a [MoveCoworkerMessage] to the opponent
     *
     * @param srcX is the x position from which the worker is being moved
     * @param srcY is the y position from which the worker is being moved
     * @param destX is the x position to which the worker is being moved
     * @param destY is the y position to which the worker is being moved
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendPlaceWorker(srcX: Int, srcY: Int, destX: Int, destY: Int) {
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }
        /** determing jobEnum for soruce position **/
        val start: WorkerTriple = when {
            srcX == srcY && srcX == -102 -> { WorkerTriple(0, 0, JobEnum.MANAGER ) }
            // where do they place them?
            srcX == srcY && srcX == -103 -> { WorkerTriple(999, 999, JobEnum.CASHIER ) }
            // where do they place them?
            srcX == srcY && srcX == -104 -> { WorkerTriple(999, 999, JobEnum.KEEPER ) }
            else -> { WorkerTriple(srcX, srcY, JobEnum.TRAINER ) }
        }
        /** determing jobEnum for destination position **/
        val dest: WorkerTriple = when {
            destX == destY && destX == -102 -> { WorkerTriple(0, 0, JobEnum.MANAGER ) }
            // where do they place them?
            destX == destY && destX == -103 -> { WorkerTriple(999, 999, JobEnum.CASHIER ) }
            // where do they place them?
            destX == destY && destX == -104 -> { WorkerTriple(999, 999, JobEnum.KEEPER ) }
            else -> { WorkerTriple(destX, destY, JobEnum.TRAINER ) }
        }
        /**create TakeTruckMessage **/
        val message = MoveCoworkerMessage(start, dest)
        /**send message **/

        rootService.gameService.determineNextPlayer(false)

        determineNextPlayer()

        client?.sendGameActionMessage(message)

    }

    /**
     * play the opponent's turn by handling the [MoveCoworkerMessage] sent through the server.
     * transferring the workers to their given position.
     *
     * @param message the message to handle
     *
     * @throws IllegalStateException if not currently expecting an opponent's turn
     * @throws IllegalStateException if there is no game running
     **/
    fun receivePlaceWorker(message: MoveCoworkerMessage) {
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        checkNotNull(rootService.currentGame) { "somehow the current game doesnt exist." }

        val srcWorker: WorkerTriple = message.start
        val destWorker: WorkerTriple = message.destination

        /** map src jobEnum to our magic numbers **/
        val source = when(srcWorker.jobEnum) {
            JobEnum.MANAGER -> { Pair(-102, -102) }
            JobEnum.CASHIER -> { Pair(-103, -103) }
            JobEnum.KEEPER -> { Pair(-104, -104) }
            JobEnum.TRAINER -> { Pair(srcWorker.x, srcWorker.y) }
        }
        /** map dest jobEnum to our magic numbers **/
        val dest = when(destWorker.jobEnum) {
            JobEnum.MANAGER -> { Pair(-102, -102) }
            JobEnum.CASHIER -> { Pair(-103, -103) }
            JobEnum.KEEPER -> {Pair(-104, -104) }
            JobEnum.TRAINER -> {Pair(destWorker.x, destWorker.y) }
        }

        rootService.playerActionService.moveEmployee(
            source.first, source.second, dest.first, dest.second, PlayerType.NETWORK)

        rootService.gameService.determineNextPlayer(false)

        determineNextPlayer()
    }

    /**
     * send a [MoveTileMessage] to the opponent
     *
     * @param playerName is the name of the player from which a prisoner is being taken
     * @param x is the x position to which the prisoner is being moved
     * @param y is the y position to which the prisoner is being moved
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendMoveTile(playerName: String, x: Int, y: Int){
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }

        val listsToSend = prepareLists()

        /**create TakeTruckMessage **/
        val message = MoveTileMessage(
            playerName,
            PositionPair(x,y),
            listsToSend.second.toList(),
            listsToSend.third.toList()
        )
        /**send message **/
        client?.sendGameActionMessage(message)

        rootService.gameService.determineNextPlayer(false)

        determineNextPlayer()
        resetLists()
    }

    /**
     * play the opponent's turn by handling the [MoveTileMessage] sent through the server.
     * transferring the workers to their given position.
     *
     * @param message the message to handle
     *
     * @throws IllegalStateException if not currently expecting an opponent's turn
     * @throws IllegalStateException if there is no game running
     **/
    fun receiveMoveTile(message: MoveTileMessage){
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        val game = rootService.currentGame
        checkNotNull(game) { "somehow the current game doesnt exist." }

        val currentPlayer: Player = game.players[game.currentPlayer]
        val fromPlayer: Player = game.players.filter { it.name == message.playerName }[0]
        val posX: Int = message.position.x
        val posY: Int = message.position.y
        val dest: Pair<Int, Int> = if (posX == posY && posY == 0) Pair(-100,-100) else Pair(posX, posY)
        val child: Pair<Boolean, PrisonerTile?>
        val possibleChildren: MutableList<Pair<Boolean, PrisonerTile?>> = mutableListOf()

        if (message.playerName == currentPlayer.name) {
            child = rootService.playerActionService.movePrisonerToPrisonYard(
                posX,
                posY
            )
            possibleChildren.add(child)
        } else {
            child = rootService.playerActionService.buyPrisonerFromOtherIsolation(
                fromPlayer,
                dest.first,
                dest.second
            )
            possibleChildren.add(child)
        }

        placeChildren(message.offspringList, possibleChildren)
        placeWorker(message.workerList)

        rootService.gameService.determineNextPlayer(false)

        determineNextPlayer()
    }

    /**
     * send a [DiscardMessage] to the opponent
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendDiscard(){
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }

        /**create TakeTruckMessage **/
        val message = DiscardMessage()
        /**send message **/
        client?.sendGameActionMessage(message)

        determineNextPlayer()
    }

    /**
     * play the opponent's turn by handling the [DiscardMessage] sent through the server.
     * discarding the top card from isolation.
     *
     * @param message the message to handle
     *
     * @throws IllegalStateException if not currently expecting an opponent's turn
     * @throws IllegalStateException if there is no game running
     **/
    fun receiveDiscard(){
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        val game = rootService.currentGame
        checkNotNull(game) { "somehow the current game doesnt exist." }

        rootService.playerActionService.freePrisoner(PlayerType.NETWORK)

        determineNextPlayer()
    }

    /**
     * Updates the [connectionState] to [newState] and notifies
     * all refreshables via [Refreshable.refreshConnectionState]
     */
    fun updateConnectionState(newState: ConnectionState) {
        this.connectionState = newState
        onAllRefreshables {
            refreshConnectionState(newState)
        }

    }

    /**
     * [increasePrisoners] adds a new prisoner element to the prisoners list.
     * - first -> x coordinate
     * - second -> y coordinate
     * - third -> index of bus place
     **/
    fun increasePrisoners(prisoner: Triple<Int, Int, Int>) { prisoners.add(prisoner) }

    /**
     * [increaseChildren] adds a new child element to the prisoners list.
     * - first -> x coordinate
     * - second -> y coordinate
     * - third -> prisoner tile
     **/
    fun increaseChildren(child: Triple<Int, Int, PrisonerTile>) { children.add(child) }

    /**
     * [increaseWorkers] adds a new worker element to the prisoners list.
     * - first -> x coordinate
     * - second -> y coordinate
     * - third -> guard tile
     **/
    fun increaseWorkers(worker: Pair<Int, Int>) { workers.add(worker) }

    /**
     * [increasePlayersJoined] adds a new player to the playersJoined list.
     **/
    fun increasePlayersJoined(player: String) {
        playersJoined.add(player)
        val newPlayer = Player(player, PlayerType.NETWORK)
        onAllRefreshables {
            refreshAfterPlayerJoined(newPlayer)
        }
    }

    /**
     * [resetLists] resets all network list.
     **/
    private fun resetLists() {
        prisoners.clear()
        children.clear()
        workers.clear()
    }

    /**
     * [prepareLists] transforms all elements from network mutable lists to list
     * that can bei send to another network player.
     *
     * @return a triple that holds lists of animals (first),
     * list of offsprings (second), list of workers (third)
     **/
    fun prepareLists():
            Triple<MutableList<AnimalTriple>,
            MutableList<OffspringTriple>,
            MutableList<WorkerTriple>>
    {
        val animalList: MutableList<AnimalTriple> = mutableListOf()
        val childrenList: MutableList<OffspringTriple> = mutableListOf()
        val workerList: MutableList<WorkerTriple> = mutableListOf()
        /** get all tiles and positions of tiles being placed **/
        prisoners.forEach { prisoner ->
            if (prisoner.first == prisoner.second && prisoner.first == -100) { //place on depot
                animalList.add(AnimalTriple(0, 0, prisoner.third))
            } else { //place on field
                animalList.add(AnimalTriple(prisoner.first, prisoner.second, prisoner.third))
            }
        }
        /** get all children and positions of tiles being placed **/
        children.forEach { child ->
            if (child.first == child.second && child.first == -100) { //place on depot
                childrenList.add(OffspringTriple(0, 0, child.third.id))
            } else {
                childrenList.add(OffspringTriple(child.first, child.second, child.third.id))
            }
        }
        /** get all workers and positions of workers being placed **/
        workers.forEach { worker ->
            if (worker.first == worker.second) {
                when(worker.first) {
                    -102 -> { workerList.add(WorkerTriple(0, 0, JobEnum.MANAGER )) }
                    // where do they place them?
                    -103 -> { workerList.add(WorkerTriple(999, 999, JobEnum.CASHIER )) }
                    // where do they place them?
                    -104 -> { workerList.add(WorkerTriple(999, 999, JobEnum.KEEPER )) }
                    else -> { workerList.add(WorkerTriple(worker.first, worker.second, JobEnum.TRAINER )) }
                }
            }
            else { workerList.add(WorkerTriple(worker.first, worker.second, JobEnum.TRAINER )) }
        }

        return Triple(animalList, childrenList, workerList)
    }

    /**
     * [placeChildren] places the children given in the offspringList.
     *
     * @param offspringList is a list that holds coordinates for all children to be placed
     *
     * @throws IllegalStateException when no game is running
     **/
    private fun placeChildren(
        offspringList: List<OffspringTriple>,
        possibleChildren: MutableList<Pair<Boolean, PrisonerTile?>>
    ) {
        val game = rootService.currentGame
        val childrenToPlace: MutableList<Triple<PrisonerTile?, Int, Int>> = mutableListOf()

        checkNotNull(game) { "somehow the current game doesnt exist." }
        /** handle children and place them **/

        possibleChildren.forEach { child ->
            val childTile = child.second
            val childId: Int

            if (childTile == null) { return@forEach } else { childId = childTile.id }
            offspringList.forEach { offspring ->
                if (childId == offspring.tileId){
                    childrenToPlace.add(Triple(childTile, offspring.x, offspring.y))
                }
            }
        }

        childrenToPlace.forEach { offsprings ->
            val offspringTile = offsprings.first
            check(offspringTile is PrisonerTile) { "the given tile is not a Prisoner" }

            if (offsprings.second == 0 && offsprings.third == 0) {
                rootService.playerActionService.placePrisoner(offspringTile, -100, -100)
            } else {
                rootService.playerActionService.placePrisoner(offspringTile, offsprings.second, offsprings.third)
            }
        }
    }

    /**
     * [placeWorker] places the workers given in the workerList.
     *
     * @param workerList is a list that holds coordinates for all workers to be placed
     *
     * @throws IllegalStateException when no game is running
     **/
    private fun placeWorker(workerList: List<WorkerTriple>) {
        val game = rootService.currentGame

        checkNotNull(game)
        /** handle workers and place them **/
        workerList.forEach { workers ->
            when(workers.jobEnum){
                JobEnum.MANAGER -> { rootService.playerActionService.moveEmployee(
                    -101, -101, -102, -102, PlayerType.NETWORK )
                }
                JobEnum.CASHIER -> { rootService.playerActionService.moveEmployee(
                    -101, -101, -103, -103, PlayerType.NETWORK )
                }
                JobEnum.KEEPER -> { rootService.playerActionService.moveEmployee(
                    -101, -101, -104, -104, PlayerType.NETWORK )
                }
                JobEnum.TRAINER -> { rootService.playerActionService.moveEmployee(
                    -101, -101, workers.x, workers.y, PlayerType.NETWORK)
                }
            }
        }
    }

    /**
     * [determineNextPlayer] is a function that helps to determine the next player
     * after a service action in the network
     * **/
    private fun determineNextPlayer() {
        val gameAfterSave = rootService.currentGame
        checkNotNull(gameAfterSave) { "game was quit" }
        val currentPlayer = gameAfterSave.players[gameAfterSave.currentPlayer]

        if (hostPlayer == null) {
            when(currentPlayer.name) {
                servicePlayer -> {
                    updateConnectionState(ConnectionState.PLAYING_MY_TURN)
                }
                else -> {
                    updateConnectionState(ConnectionState.WAITING_FOR_TURN)
                }
            }
        } else {
            when(currentPlayer.name) {
                hostPlayer -> {
                    updateConnectionState(ConnectionState.PLAYING_MY_TURN)
                }
                else -> {
                    updateConnectionState(ConnectionState.WAITING_FOR_TURN)
                }
            }
        }
    }

}
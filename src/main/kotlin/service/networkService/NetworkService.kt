package service.networkService

import edu.udo.cs.sopra.ntf.AddTileToTruckMessage
import entity.Player
import entity.PrisonBus
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.AbstractRefreshingService
import service.RootService
import edu.udo.cs.sopra.ntf.InitGameMessage
import edu.udo.cs.sopra.ntf.TakeTruckMessage
import edu.udo.cs.sopra.ntf.MoveTileMessage
import edu.udo.cs.sopra.ntf.BuyExpansionMessage
import edu.udo.cs.sopra.ntf.MoveCoworkerMessage
import edu.udo.cs.sopra.ntf.DiscardMessage
import entity.AquaGhetto
import entity.enums.PlayerType
import java.util.*

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
     * @param hostPlayerName player name of the host player
     * @param guestPlayerName player name of the guest player
     *
     * @throws IllegalStateException if [connectionState] != [ConnectionState.WAITING_FOR_GUEST]
     */
    fun startNewHostedGame(hostPlayerName: String, guestPlayers: MutableList<String>) {
        check(connectionState == ConnectionState.WAITING_FOR_GUEST)
        { "currently not prepared to start a new hosted game." }

        val game: AquaGhetto? = rootService.currentGame
        val players: MutableList<Pair<String, PlayerType>> = mutableListOf()
        val messagePlayerList: MutableList<String> = mutableListOf()
        val drawStackList: MutableList<Int> = mutableListOf()
        players.add(Pair(hostPlayerName, PlayerType.PLAYER))
        messagePlayerList.add(hostPlayerName)

        guestPlayers.forEach { playerName ->
            players.add(Pair(playerName, PlayerType.PLAYER))
            messagePlayerList.add(playerName)
        }

        rootService.gameService.startNewGame(players)

        checkNotNull(game) { "game should not be null right after starting it." }

        val drawStack: Stack<Tile> = game.drawStack
        drawStack.addAll(game.finalStack)
        drawStack.forEach { tile ->
            drawStackList.add(tile.id)
        }

        val message = InitGameMessage(
            messagePlayerList.toList(),
            drawStackList.toList()
        )
        updateConnectionState(ConnectionState.PLAYING_MY_TURN)
        client?.sendGameActionMessage(message)
    }

    /**
     * Initializes the entity structure with the data given by the [InitGameMessage] sent by the host.
     * [connectionState] needs to be [ConnectionState.WAITING_FOR_INIT].
     * This method should be called from the [AqueghettoNetworkClient] when the host sends the init message.
     * See [AqueghettoNetworkClient.onInitReceived].
     *
     * @throws IllegalStateException if not currently waiting for an init message
     */
    fun startNewJoinedGame(message: InitGameMessage, players: MutableList<String>) {
        check(connectionState == ConnectionState.WAITING_FOR_INIT)
        { "not waiting for game init message." }


        val game = AquaGhetto()
        /*Create list of players*/
        val playerList = mutableListOf<Player>()
        val finalStackIndex: Int = message.drawPile.size-16
        val drawStack: Stack<Tile> = Stack()
        val finalStack: Stack<Tile> = Stack()

        rootService.currentGame = game

        players.forEach { playerName ->
            val player = Player(playerName, PlayerType.PLAYER)
            playerList.add(player)
            for (x in 1..4) {
                for (y in 1..4) {
                    if (x == 4 && y == 4) continue
                    player.board.setPrisonGrid(x, y, true)
                }
            }
            player.board.setPrisonGrid(0, 2, true)
            player.board.setPrisonGrid(0, 3, true)
            player.board.setPrisonGrid(2, 0, true)
            player.board.setPrisonGrid(3, 0, true)
        }

        game.players = playerList
        rootService.boardService.createAllTiles()

        message.drawPile.forEachIndexed { index, tileId ->
            if (index >= finalStackIndex) {
               finalStack.add(game.allTiles.get(tileId))
            } else {
                drawStack.add(game.allTiles.get(tileId))
            }
        }
        game.drawStack = drawStack
        game.finalStack = finalStack

        /*Create prisonBusses*/
        game.prisonBuses = rootService.boardService.createPrisonBuses(playerList.size)

        updateConnectionState(ConnectionState.PLAYING_MY_TURN)
        onAllRefreshables {
            refreshAfterStartGame()
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }

    }

    /**
     * Connects to server and joins a game session as guest player.
     *
     * @param secret Server secret.
     * @param name Player name.
     * @param sessionID identifier of the joined session (as defined by host on create)
     *
     * @throws IllegalStateException if already connected to another game or connection attempt fails
     */
    fun joinGame(secret: String, name: String, sessionID: String) {
        if (!connect(secret, name)) {
            error("Connection failed")
        }
        updateConnectionState(ConnectionState.CONNECTED)

        client?.joinGame(sessionID, "Hello!")

        updateConnectionState(ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
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

        if (sessionID.isNullOrBlank()) {
            client?.createGame(GAME_ID, "Welcome!")
        } else {
            client?.createGame(GAME_ID, sessionID, "Welcome!")
        }
        updateConnectionState(ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
    }


    fun sendAddTileToTruck(tile: Tile, prisonBus: PrisonBus) {}

    fun receiveAddTileToTruck(message: AddTileToTruckMessage) {}

    fun sendTakeTruck(prisonBus: PrisonBus, bonuses: MutableList<Tile>) {}

    fun receiveTakeTruck(message: TakeTruckMessage) {}

    fun sendBuyExpansion(isBigExpansion: Boolean, x: Int, y: Int, rotation: Int) {}

    fun receiveBuyExpansion(message: BuyExpansionMessage) {}

    fun sendPlaceWorker(srcX: Int, srcY: Int, destX: Int, destY: Int ) {}

    fun receivePlaceWorker(message: MoveCoworkerMessage) {}

    fun sendMoveTile(playerName: String, tile: PrisonerTile, x: Int, y: Int){}

    fun receiveMoveTile(message: MoveTileMessage){}

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

}
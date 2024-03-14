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
import entity.tileTypes.CoinTile
import entity.tileTypes.GuardTile
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
     * lists to hold elements that need to be sent when taking a bus
     **/
    val prisoners: MutableList<Triple<Int, Int, Int>> = mutableListOf()
    val children: MutableList<Triple<Int, Int, Tile>> = mutableListOf()
    val workers: MutableList<Triple<Int, Int, GuardTile>> = mutableListOf()

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

        val players: MutableList<Pair<String, PlayerType>> = mutableListOf()
        val messagePlayerList: MutableList<String> = mutableListOf()
        val drawStackList: MutableList<Int> = mutableListOf()

        /**Add hostPlayer to playersList and to messagePlayerList **/
        players.add(Pair(hostPlayerName, PlayerType.PLAYER))
        messagePlayerList.add(hostPlayerName)

        /**Add guestPlayers to players and to messagePlayerList **/
        guestPlayers.forEach { playerName ->
            players.add(Pair(playerName, PlayerType.PLAYER))
            messagePlayerList.add(playerName)
        }

        rootService.gameService.startNewGame(players)

        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "game should not be null right after starting it." }
        check(game.drawStack.size > 0) { "the game was not initialized properly." }
        check(game.finalStack.size > 0) { "the game was not initialized properly." }

        /**Combine both stack to one**/
        val currentPlayer = game.players[game.currentPlayer]
        val drawStack: Stack<Tile> = Stack()
        drawStack.addAll(game.drawStack)
        drawStack.addAll(game.finalStack)

        drawStack.forEachIndexed { index, tile -> drawStackList.add(tile.id) }

        val message = InitGameMessage(
            messagePlayerList.toList(),
            drawStackList.toList()
        )

        when(currentPlayer.name) {
            hostPlayerName -> updateConnectionState(ConnectionState.PLAYING_MY_TURN)
            else -> updateConnectionState(ConnectionState.WAITING_FOR_TURN)
        }

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

        val currentPlayer = game.players[game.currentPlayer]

        message.drawPile.forEachIndexed { index, tileId ->
            if (index >= finalStackIndex) {
               finalStack.add(game.allTiles[tileId-1])
            } else {
                drawStack.add(game.allTiles[tileId-1])
            }
        }
        game.drawStack = drawStack
        game.finalStack = finalStack

        /*Create prisonBusses*/
        game.prisonBuses = rootService.boardService.createPrisonBuses(playerList.size)

        when(currentPlayer.name) {
            sender -> updateConnectionState(ConnectionState.PLAYING_MY_TURN)
            else -> updateConnectionState(ConnectionState.WAITING_FOR_TURN)
        }

        onAllRefreshables {
            refreshAfterStartGame()
            refreshAfterNextTurn(currentPlayer)
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

    /**
     * send a [AddTileToTruckMessage] to the opponent
     *
     * @param prisonBus is the prison bus where the tile is being placed
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendAddTileToTruck(prisonBus: PrisonBus) {
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        var selectedPrisonBus: Int = 0
        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }

        game.prisonBuses.forEachIndexed { index, bus ->
            if (bus == prisonBus) { selectedPrisonBus = index }
        }

        val message = AddTileToTruckMessage(selectedPrisonBus)
        client?.sendGameActionMessage(message)
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
    fun receiveAddTileToTruck(message: AddTileToTruckMessage) {
        check(connectionState == ConnectionState.WAITING_FOR_TURN) {
            "currently not expecting an opponent's turn."
        }

        val game = rootService.currentGame
        val busId: Int = message.truckId

        checkNotNull(game) { "somehow the current game doesnt exist." }
        check(busId in 0 until  game.prisonBuses.size) { "there is no bus at this index" }

        val tileToAdd: Tile = rootService.playerActionService.drawCard()
        val prisonBus: PrisonBus = game.prisonBuses[busId]

        rootService.playerActionService.addTileToPrisonBus(tileToAdd, prisonBus)
    }

    /**
     * send a [TakeTruckMessage] to the opponent
     *
     * @param prisonBus is the prison bus that is being taken
     *
     * @throws IllegalArgumentException if it's not currently my turn
     * @throws IllegalStateException if there is no game running
     */
    fun sendTakeTruck(prisonBus: PrisonBus) {
        require(connectionState == ConnectionState.PLAYING_MY_TURN) { "not my turn" }

        var selectedPrisonBus: Int = 0
        val game = rootService.currentGame

        checkNotNull(game) { "somehow the current game doesnt exist." }
        /** get the id from given bus **/
        game.prisonBuses.forEachIndexed { index, bus ->
            if (bus == prisonBus) { selectedPrisonBus = index }
        }

        val listsToSend = prepareLists()
        /**create TakeTruckMessage **/
        val message = TakeTruckMessage(
            selectedPrisonBus,
            listsToSend.first.toList(),
            listsToSend.second.toList(),
            listsToSend.third.toList()
        )
        /**send message **/
        client?.sendGameActionMessage(message)
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
        val prisonBus: PrisonBus = game.prisonBuses[busId]
        val takenBus: PrisonBus? = currentPlayer.takenBus

        rootService.playerActionService.takePrisonBus(prisonBus)

        requireNotNull(takenBus) { "the player has a wrong bus" }
        require(currentPlayer.takenBus == prisonBus) { "the player has a wrong bus" }

        /** handle tiles on truck and place them **/
        message.animalList.forEach {animals ->
            val tileToPlace: Int? = takenBus.tiles[animals.truck]?.id

            require(!takenBus.blockedSlots[animals.truck]) { "this slot is blocked" }
            requireNotNull(tileToPlace) { "tile does not exist" }

            val tile =  game.allTiles[tileToPlace-1]
            if (tile is PrisonerTile){
                if (animals.x == 0 && animals.y == 0) {
                    rootService.playerActionService.placePrisoner(tile, -100, -100)
                } else {
                    rootService.playerActionService.placePrisoner(tile, animals.x, animals.y)
                }
            }
            if (tile is CoinTile) { currentPlayer.coins += 1 }
        }

        placeChildren(message.offspringList)
        placeWorker(message.workerList)
    }

    fun sendBuyExpansion(isBigExpansion: Boolean, x: Int, y: Int, rotation: Int) {}

    fun receiveBuyExpansion(message: BuyExpansionMessage) {

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
    fun sendPlaceWorker(srcX: Int, srcY: Int, destX: Int, destY: Int ) {
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
            else -> { WorkerTriple(srcX, srcY, JobEnum.TRAINER ) }
        }
        /**create TakeTruckMessage **/
        val message = MoveCoworkerMessage(start, dest)
        /**send message **/
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

        var source: Pair<Int, Int> = Pair(0, 0)
        var dest: Pair<Int, Int> = Pair(0, 0)
        /** map src jobEnum to our magic numbers **/
        source = when(srcWorker.jobEnum) {
            JobEnum.MANAGER -> { Pair(-102, -102) }
            JobEnum.CASHIER -> { Pair(-103, -103) }
            JobEnum.KEEPER -> { Pair(-104, -104) }
            JobEnum.TRAINER -> { Pair(srcWorker.x, srcWorker.y) }
        }
        /** map dest jobEnum to our magic numbers **/
        dest = when(destWorker.jobEnum) {
            JobEnum.MANAGER -> { Pair(-102, -102) }
            JobEnum.CASHIER -> { Pair(-103, -103) }
            JobEnum.KEEPER -> {Pair(-104, -104) }
            JobEnum.TRAINER -> {Pair(srcWorker.x, srcWorker.y) }
        }

        rootService.playerActionService.moveEmployee(source.first, source.second, dest.first, dest.second)
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

        if (message.playerName == currentPlayer.name) {
            rootService.playerActionService.movePrisonerToPrisonYard(
                posX,
                posY
            )
        } else {
            rootService.playerActionService.buyPrisonerFromOtherIsolation(
                fromPlayer,
                dest.first,
                dest.second
            )
        }

        placeChildren(message.offspringList)
        placeWorker(message.workerList)
    }

    fun sendDiscard(id: Int){}
    fun receiveDiscard(message: DiscardMessage){

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
     **/
    fun increasePrisoners(prisoner: Triple<Int, Int, Int>) { prisoners.add(prisoner) }

    /**
     * [increaseChildren] adds a new child element to the prisoners list.
     **/
    fun increaseChildren(child: Triple<Int, Int, Tile>) { children.add(child) }

    /**
     * [increaseWorkers] adds a new worker element to the prisoners list.
     **/
    fun increaseWorkers(worker: Triple<Int, Int, GuardTile>) { workers.add(worker) }

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
    private fun prepareLists():
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
    private fun placeChildren(offspringList: List<OffspringTriple>) {
        val game = rootService.currentGame
        checkNotNull(game) { "somehow the current game doesnt exist." }
        /** handle children and place them **/
        offspringList.forEach { offsprings ->
            val childToPlace: Tile = game.allTiles[offsprings.tileId-1]
            require(childToPlace is PrisonerTile) { "the given tile is not a Prisoner" }

            if (offsprings.x == 0 && offsprings.y == 0) {
                rootService.playerActionService.placePrisoner(childToPlace, -100, -100)
            } else {
                rootService.playerActionService.placePrisoner(childToPlace, offsprings.x, offsprings.y)
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
        /** handle workers and place them **/
        workerList.forEach { workers ->
            when(workers.jobEnum){
                JobEnum.MANAGER -> { rootService.playerActionService.moveEmployee(
                    -102, -102, -102, -102 )
                }
                JobEnum.CASHIER -> { rootService.playerActionService.moveEmployee(
                    -103, -103, -103, -103 )
                }
                JobEnum.KEEPER -> { rootService.playerActionService.moveEmployee(
                    -104, -104, -104, -104 )
                }
                JobEnum.TRAINER -> { rootService.playerActionService.moveEmployee(
                    -101, -101, workers.x, workers.y )
                }
            }
        }
    }

}
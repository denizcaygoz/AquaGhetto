package service.networkService

import edu.udo.cs.sopra.ntf.*
import tools.aqua.bgw.net.client.BoardGameClient
import tools.aqua.bgw.net.client.NetworkLogging
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.net.common.annotations.GameActionReceiver
import tools.aqua.bgw.net.common.notification.PlayerJoinedNotification
import tools.aqua.bgw.net.common.response.*

class AqueghettoNetworkClient(
    playerName: String,
    host: String,
    secret: String,
    var networkService: NetworkService,
): BoardGameClient(
    playerName,
    host,
    secret,
    NetworkLogging.VERBOSE
) {

    /** the identifier of this game session; can be null if no session started yet. */
    var sessionID: String? = null

    /** the name of the opponent player; can be null if no message from the opponent received yet */
    var otherPlayerNames: MutableList<String> = mutableListOf()


    /**
     * Handle a [CreateGameResponse] sent by the server. Will await the guest player when its
     * status is [CreateGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a game creation response.
     */
    override fun onCreateGameResponse(response: CreateGameResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_HOST_CONFIRMATION)
            { "unexpected CreateGameResponse" }

            when (response.status) {
                CreateGameResponseStatus.SUCCESS -> {
                    networkService.updateConnectionState(ConnectionState.WAITING_FOR_GUEST)
                    sessionID = response.sessionID
                }
                else -> disconnectAndError(response.status)
            }
        }
    }

    /**
     * Handle a [JoinGameResponse] sent by the server. Will await the init message when its
     * status is [JoinGameResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     *
     * @throws IllegalStateException if status != success or currently not waiting for a join game response.
     */
    override fun onJoinGameResponse(response: JoinGameResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_JOIN_CONFIRMATION)
            { "unexpected JoinGameResponse" }

            when (response.status) {
                JoinGameResponseStatus.SUCCESS -> {
                    otherPlayerNames.add(response.opponents[0])
                    sessionID = response.sessionID
                    networkService.updateConnectionState(ConnectionState.WAITING_FOR_INIT)
                }
                else -> {
                    disconnectAndError(response.status)
                }
            }
        }

    }

    /**
     * Handle a [PlayerJoinedNotification] sent by the server. As War only supports two players,
     * this will immediately start the hosted game (and send the init message to the opponent).
     *
     * @throws IllegalStateException if not currently expecting any guests to join.
     */
    override fun onPlayerJoined(notification: PlayerJoinedNotification) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.WAITING_FOR_GUEST )
            { "not awaiting any guests."}

            otherPlayerNames.add(notification.sender)

            networkService.startNewHostedGame(playerName, otherPlayerNames)
        }
    }

    /**
     * Handle a [GameActionResponse] sent by the server. Does nothing when its
     * status is [GameActionResponseStatus.SUCCESS]. As recovery from network problems is not
     * implemented in NetWar, the method disconnects from the server and throws an
     * [IllegalStateException] otherwise.
     */
    override fun onGameActionResponse(response: GameActionResponse) {
        BoardGameApplication.runOnGUIThread {
            check(networkService.connectionState == ConnectionState.PLAYING_MY_TURN ||
                    networkService.connectionState == ConnectionState.WAITING_FOR_TURN)
            { "not currently playing in a network game."}

            when (response.status) {
                GameActionResponseStatus.SUCCESS -> {} // do nothing in this case
                else -> disconnectAndError(response.status)
            }
        }
    }

    /**
     * handle a [InitGameMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onInitReceived(message: InitGameMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.startNewJoinedGame(message, playerName)
        }
    }

    /**
     * handle a [AddTileToTruckMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onAddTileToTruck(message: AddTileToTruckMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveAddTileToTruck(message)
        }
    }

    /**
     * handle a [TakeTruckMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onTakeTruck(message: TakeTruckMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveTakeTruck(message)
        }
    }

    /**
     * handle a [BuyExpansionMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onBuyExpansion(message: BuyExpansionMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveBuyExpansion(message)
        }
    }

    /**
     * handle a [MoveCoworkerMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onMoveCoworker(message: MoveCoworkerMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receivePlaceWorker(message)
        }
    }

    /**
     * handle a [MoveTileMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onMoveTile(message: MoveTileMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveMoveTile(message)
        }
    }

    /**
     * handle a [DiscardMessage] sent by the server
     */
    @Suppress("UNUSED_PARAMETER", "unused")
    @GameActionReceiver
    fun onDiscard(message: DiscardMessage, sender: String) {
        BoardGameApplication.runOnGUIThread {
            networkService.receiveDiscard(message)
        }
    }

    private fun disconnectAndError(message: Any) {
        networkService.disconnect()
        error(message)
    }


}
package service.network

import service.RootService
import service.networkService.ConnectionState
import kotlin.test.*

/**
 * [ConnectionTest] test setting up a connection to the Network
 **/
class ConnectionTest {
    private lateinit var rootServiceHost: RootService
    private lateinit var rootServiceGuest: RootService
    private lateinit var rootServiceAnotherGuest: RootService

    companion object {
        const val NETWORK_SECRET = "aqua24a"
    }


    /**
     * Initialize both connections and start the game, so that the players of both games
     * (represented by [rootServiceHost] and [rootServiceGuest]) are in their turns.
     */
    private fun initConnections() {
        rootServiceHost = RootService()
        rootServiceGuest = RootService()

        val sessionID = rootServiceHost.networkService.createSessionID()

        rootServiceHost.networkService.hostGame(NETWORK_SECRET, "Test Host", sessionID)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_GUEST)


        rootServiceGuest.networkService.joinGame(NETWORK_SECRET, "Test Guest", sessionID)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_INIT)

        rootServiceHost.networkService.startNewHostedGame()

        rootServiceHost.waitForState(ConnectionState.PLAYING_MY_TURN)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_TURN)
    }

    private fun initMultipleConnections() {
        rootServiceHost = RootService()
        rootServiceGuest = RootService()
        rootServiceAnotherGuest = RootService()

        rootServiceHost.networkService.hostGame(NETWORK_SECRET, "Frank", null)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_GUEST)

        val sessionID = rootServiceHost.networkService.client?.sessionID

        assertNotNull(sessionID)

        rootServiceGuest.networkService.joinGame(NETWORK_SECRET, "Test Guest", sessionID)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_INIT)


        rootServiceAnotherGuest.networkService.joinGame(NETWORK_SECRET, "Test Another", sessionID)
        rootServiceAnotherGuest.waitForState(ConnectionState.WAITING_FOR_INIT)
    }

    @Test
    fun testPlayersJoining() {
        initMultipleConnections()

        rootServiceHost.networkService.startNewHostedGame()

        rootServiceAnotherGuest.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_TURN)
    }

    /**
     * test that the players' stacks of both games are in the same state after starting the game.
     */
    @Test
    fun testHostAndJoinGame() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        assertTrue{ hostGame.drawStack.size == guestGame.drawStack.size }
        assertTrue{ hostGame.finalStack.size == guestGame.finalStack.size }

        assertTrue { hostGame.drawStack.pop().id == guestGame.drawStack.pop().id }
        assertTrue { hostGame.finalStack.pop().id == guestGame.finalStack.pop().id }
    }

    /**
     * Test disconnecting after starting the game. State expected to be [ConnectionState.DISCONNECTED]
     * afterward and the [NetworkService.client]s must be null.
     */
    @Test
    fun testDisconnect() {
        initConnections()

        rootServiceHost.networkService.disconnect()
        rootServiceGuest.networkService.disconnect()

        rootServiceHost.waitForState(ConnectionState.DISCONNECTED)
        rootServiceGuest.waitForState(ConnectionState.DISCONNECTED)

        assertNull(rootServiceHost.networkService.client)
        assertNull(rootServiceGuest.networkService.client)
    }


    /**
     * Tests if the connection fails if the wrong server secret is provided.
     */
    @Test
    fun testWrongSecret() {
        rootServiceHost = RootService()
        assertFailsWith<IllegalStateException> {
            rootServiceHost.networkService.hostGame("thiswillneverbethesecret", "Test", null)
        }
    }

    /**
     * busy waiting for the game represented by this [RootService] to reach the desired network [state].
     * Polls the desired state every 100 ms until the [timeout] is reached.
     *
     * This is a simplification hack for testing purposes, so that tests can be linearized on
     * a single thread.
     *
     * @param state the desired network state to reach
     * @param timeout maximum milliseconds to wait (default: 5000)
     *
     * @throws IllegalStateException if desired state is not reached within the [timeout]
     */
    private fun RootService.waitForState(state: ConnectionState, timeout: Int = 5000) {
        var timePassed = 0
        while (timePassed < timeout) {
            if (networkService.connectionState == state)
                return
            else {
                Thread.sleep(100)
                timePassed += 100
            }
        }
        error("Did not arrive at state $state after waiting $timeout ms")
    }
}
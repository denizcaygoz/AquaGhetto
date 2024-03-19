package service.network

import edu.udo.cs.sopra.ntf.BuyExpansionMessage
import edu.udo.cs.sopra.ntf.PositionPair
import org.junit.jupiter.api.assertThrows
import service.RootService
import service.networkService.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [BuyExpansionTest] tests the GameActionMessage of buying an expansion.
 **/
class BuyExpansionTest {
    private lateinit var rootServiceHost: RootService
    private lateinit var rootServiceGuest: RootService

    /**
     * Initialize both connections and start the game, so that the players of both games
     * (represented by [rootServiceHost] and [rootServiceGuest]) are in their turns.
     */
    private fun initConnections() {
        rootServiceHost = RootService()
        rootServiceGuest = RootService()

        val sessionID = rootServiceHost.networkService.createSessionID()

        rootServiceHost.networkService.hostGame(ConnectionTest.NETWORK_SECRET, "Test Host", sessionID)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_GUEST)


        rootServiceGuest.networkService.joinGame(ConnectionTest.NETWORK_SECRET, "Test Guest", sessionID)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_INIT)

        rootServiceHost.networkService.startNewHostedGame()

        rootServiceHost.waitForState(ConnectionState.PLAYING_MY_TURN)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_TURN)
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

    /**
     * Tests if buying a wrong expansion works properly on the network.
     */
    @Test
    fun testWrongBuyExpansion() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val wrongMessage = BuyExpansionMessage(listOf(
            PositionPair(2,7),
            PositionPair(3,7),
            PositionPair(2,6),
            PositionPair(3,6),
            PositionPair(4,7)
        ))
        assertThrows<IllegalArgumentException> { rootServiceGuest.networkService.receiveBuyExpansion(wrongMessage) }
    }

    /**
     * Tests if buying a big expansion works properly on the network.
     */
    @Test
    fun testBuyBigExpansion() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 6
        guestCurrentPlayer.coins = 6

        rootServiceHost.playerActionService.expandPrisonGrid(true, 1,6, 0)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,5) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,5) }
    }

    /**
     * Tests if buying a small expansion with 0 rotation works properly on the network.
     */
    @Test
    fun testBuySmallExpansionOne() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        guestCurrentPlayer.coins = 20

        rootServiceHost.playerActionService.expandPrisonGrid(false, 1,6, 0)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,5) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,5) }
    }

    /**
     * Tests if buying a small expansion with 90 rotation works properly on the network.
     */
    @Test
    fun testBuySmallExpansionTwo() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        guestCurrentPlayer.coins = 20

        rootServiceHost.playerActionService.expandPrisonGrid(false, 2,6, 90)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,5) }
    }

    /**
     * Tests if buying a small expansion with 180 rotation works properly on the network.
     */
    @Test
    fun testBuySmallExpansionThree() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        guestCurrentPlayer.coins = 20

        rootServiceHost.playerActionService.expandPrisonGrid(false, 2,5, 180)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,5) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,6) }
    }

    /**
     * Tests if buying a small expansion with 270 rotation works properly on the network.
     */
    @Test
    fun testBuySmallExpansionFour() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        guestCurrentPlayer.coins = 20

        rootServiceHost.playerActionService.expandPrisonGrid(false, 1,5, 270)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { guestCurrentPlayer.board.getPrisonGrid(1,5) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,6) }
        assertTrue { guestCurrentPlayer.board.getPrisonGrid(2,5) }
    }
}
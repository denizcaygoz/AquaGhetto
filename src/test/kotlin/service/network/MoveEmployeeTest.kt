package service.network

import entity.tileTypes.GuardTile
import service.RootService
import service.networkService.ConnectionState
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [MoveEmployeeTest] tests the GameActionMessage of moving an employee.
 **/
class MoveEmployeeTest {
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
     * Tests if moving a Janitor to Secretary works properly on the network.
     */
    @Test
    fun testPlaceJanitorToSecretary() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        hostCurrentPlayer.hasJanitor = true
        guestCurrentPlayer.coins = 20
        guestCurrentPlayer.hasJanitor = true

        rootServiceHost.playerActionService.moveEmployee(-102, -102, -103,-103)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostCurrentPlayer.secretaryCount == 1 && !hostCurrentPlayer.hasJanitor }
        assertTrue { guestCurrentPlayer.secretaryCount == 1 && !guestCurrentPlayer.hasJanitor }
    }

    /**
     * Tests if moving a Secretary to Lawyer works properly on the network.
     */
    @Test
    fun testPlaceSecretaryToLawyer() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        hostCurrentPlayer.secretaryCount = 1
        guestCurrentPlayer.coins = 20
        guestCurrentPlayer.secretaryCount = 1

        rootServiceHost.playerActionService.moveEmployee(-103, -103, -104,-104)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostCurrentPlayer.secretaryCount == 0 && hostCurrentPlayer.lawyerCount == 1 }
        assertTrue { guestCurrentPlayer.secretaryCount == 0 && hostCurrentPlayer.lawyerCount == 1 }
    }

    /**
     * Tests if moving a Lawyer to Janitor works properly on the network.
     */
    @Test
    fun testPlaceLawyerToJanitor() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        hostCurrentPlayer.lawyerCount = 1
        guestCurrentPlayer.coins = 20
        guestCurrentPlayer.lawyerCount = 1

        rootServiceHost.playerActionService.moveEmployee(-104, -104, -102,-102)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostCurrentPlayer.lawyerCount == 0 && hostCurrentPlayer.hasJanitor }
        assertTrue { guestCurrentPlayer.lawyerCount == 0 && hostCurrentPlayer.hasJanitor }
    }

    /**
     * Tests if moving a Guard works properly on the network.
     */
    @Test
    fun testPlaceGuard() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        hostCurrentPlayer.coins = 20
        hostCurrentPlayer.board.setPrisonYard(2,2, GuardTile())
        guestCurrentPlayer.coins = 20
        guestCurrentPlayer.board.setPrisonYard(2,2, GuardTile())

        rootServiceHost.playerActionService.moveEmployee(2, 2, 2,3)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostCurrentPlayer.board.guardPosition[0].first == 2 }
        assertTrue { hostCurrentPlayer.board.guardPosition[0].second == 3 }
        assertTrue { guestCurrentPlayer.board.guardPosition[0].first == 2 }
        assertTrue { guestCurrentPlayer.board.guardPosition[0].second == 3 }
    }
}
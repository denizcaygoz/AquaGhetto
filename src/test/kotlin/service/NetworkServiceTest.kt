package tools.aqua.bgw.examples.war.service

import edu.udo.cs.sopra.ntf.AddTileToTruckMessage
import edu.udo.cs.sopra.ntf.DiscardMessage
import entity.PrisonBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import org.junit.jupiter.api.assertThrows
import service.RootService
import service.networkService.ConnectionState
import kotlin.test.*

/**
 * Class that provides tests for the [NetworkService]. It will connect to the
 * SoPra BGW-Net server (twice) and actually send messages through that server. This
 * might fail if the server is offline or [NETWORK_SECRET] is outdated.
 */
class NetworkServiceTest {

    private lateinit var rootServiceHost: RootService
    private lateinit var rootServiceGuest: RootService

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

        rootServiceHost.networkService.hostGame(NETWORK_SECRET, "Test Host", null)
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_GUEST)
        val sessionID = rootServiceHost.networkService.client?.sessionID
        assertNotNull(sessionID)

        rootServiceGuest.networkService.joinGame(NETWORK_SECRET, "Test Guest", sessionID)
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
     * afterwards and the [NetworkService.client]s must be null.
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
     * Tests if the adding tile to a truck works properly on the network.
     */
    @Test
    fun testAddTileToTruck() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostTileToPlace: Tile = rootServiceHost.playerActionService.drawCard()
        val hostBusToPlaceOn: PrisonBus = hostGame.prisonBuses[0]

        assertThrows<IllegalArgumentException> {
            rootServiceGuest.playerActionService.addTileToPrisonBus(hostTileToPlace, hostBusToPlaceOn) }

        val wrongMessage = AddTileToTruckMessage(4)
        assertThrows<IllegalStateException> { rootServiceGuest.networkService.receiveAddTileToTruck(wrongMessage) }

        rootServiceHost.playerActionService.addTileToPrisonBus(hostTileToPlace, hostBusToPlaceOn)

        val guestBusToPlaceOn: PrisonBus = guestGame.prisonBuses[0]

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostTileToPlace.id == guestBusToPlaceOn.tiles[0]?.id }

        rootServiceHost.currentGame = null
        rootServiceGuest.currentGame = null

        assertThrows<IllegalStateException> {
            rootServiceHost.playerActionService.addTileToPrisonBus(hostTileToPlace, hostBusToPlaceOn) }
    }

    /**
     * Tests if the discarding cards works properly on the network.
     */
    @Test
    fun testDiscard() {
        initConnections()

        assertEquals(rootServiceHost.networkService.connectionState, ConnectionState.PLAYING_MY_TURN)
        assertEquals(rootServiceGuest.networkService.connectionState, ConnectionState.WAITING_FOR_TURN)

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val hostCurrentPlayer = hostGame.players[hostGame.currentPlayer]
        val guestCurrentPlayer = guestGame.players[guestGame.currentPlayer]

        assertThrows<IllegalArgumentException> { rootServiceGuest.playerActionService.freePrisoner() }

        assertTrue { hostCurrentPlayer.name == guestCurrentPlayer.name }

        hostCurrentPlayer.isolation.add(PrisonerTile(4, PrisonerTrait.MALE, PrisonerType.RED))
        hostCurrentPlayer.isolation.add(PrisonerTile(5, PrisonerTrait.OLD, PrisonerType.BROWN))
        hostCurrentPlayer.coins = 6

        guestCurrentPlayer.isolation.add(PrisonerTile(4, PrisonerTrait.MALE, PrisonerType.RED))
        guestCurrentPlayer.isolation.add(PrisonerTile(5, PrisonerTrait.OLD, PrisonerType.BROWN))
        guestCurrentPlayer.coins = 6

        assertTrue { hostCurrentPlayer.isolation.peek().id == guestCurrentPlayer.isolation.peek().id}

        rootServiceHost.playerActionService.freePrisoner()

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostCurrentPlayer.isolation.peek().id == 4 }
        assertTrue { guestCurrentPlayer.isolation.peek().id == hostCurrentPlayer.isolation.peek().id }

        rootServiceHost.currentGame = null
        rootServiceGuest.currentGame = null

        assertThrows<IllegalStateException> { rootServiceGuest.playerActionService.freePrisoner() }

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
package service.network

import edu.udo.cs.sopra.ntf.*
import entity.PrisonBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.GuardTile
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
            rootServiceGuest.networkService.sendAddTileToTruck(hostBusToPlaceOn) }

        val wrongMessage = AddTileToTruckMessage(4)
        assertThrows<IllegalStateException> { rootServiceGuest.networkService.receiveAddTileToTruck(wrongMessage) }

        assertTrue { rootServiceHost.currentGame?.currentPlayer == rootServiceGuest.currentGame?.currentPlayer }

        rootServiceHost.playerActionService.addTileToPrisonBus(hostTileToPlace, hostBusToPlaceOn)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { rootServiceHost.currentGame?.currentPlayer == rootServiceGuest.currentGame?.currentPlayer }

        val guestBusToPlaceOn: PrisonBus = guestGame.prisonBuses[0]

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
     * Tests if taking a truck works properly on the network.
     */
    @Test
    fun testTakeTruck() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        var guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val parentMale = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val parentFemale = PrisonerTile(25, PrisonerTrait.FEMALE, PrisonerType.BLUE)
        val tileRed = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val anotherTileRed = PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.RED)
        /** prepare bus **/
        hostGame.players[0].coins = 10
        guestGame.players[0].coins = 10
        hostGame.prisonBuses[0].tiles[0] = parentMale
        guestGame.prisonBuses[0].tiles[0] = parentMale
        hostGame.prisonBuses[0].tiles[1] = parentFemale
        guestGame.prisonBuses[0].tiles[1] = parentFemale
        hostGame.prisonBuses[0].tiles[2] = tileRed
        guestGame.prisonBuses[0].tiles[2] = tileRed
        hostGame.players[0].board.setPrisonYard(3,3, anotherTileRed)
        guestGame.players[0].board.setPrisonYard(3,3, anotherTileRed)

        rootServiceHost.playerActionService.takePrisonBus(hostGame.prisonBuses[0])

        /** place tile from bus**/
        val takenBus: PrisonBus? = hostGame.players[0].takenBus

        assertNotNull(takenBus)

        val tileOne = takenBus.tiles[0]
        val tileTwo = takenBus.tiles[1]
        val tileThree = takenBus.tiles[2]

        assertNotNull(tileOne)
        assertNotNull(tileTwo)
        assertNotNull(tileThree)

        tileOne as PrisonerTile
        tileTwo as PrisonerTile
        tileThree as PrisonerTile

        rootServiceHost.playerActionService.placePrisoner(tileOne,1,1)
        rootServiceHost.networkService.increasePrisoners(Triple(1,1, 0))
        /** place tile from bus and get children **/
        val child = rootServiceHost.playerActionService.placePrisoner(tileTwo,2,1)
        val childToPlace = child.second
        if (childToPlace is PrisonerTile) {
            rootServiceHost.networkService.increasePrisoners(Triple(2,1, 1))
            rootServiceHost.playerActionService.placePrisoner(childToPlace,1,2)
        }
        /** place tile from bus and get worker **/
        rootServiceHost.playerActionService.placePrisoner(tileThree,3,2)
        rootServiceHost.networkService.increasePrisoners(Triple(3,2, 2))
        rootServiceHost.playerActionService.moveEmployee(-101,-101, 2,2)
        /** send message to other player **/
        rootServiceHost.networkService.sendTakeTruck(0)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        guestGame = rootServiceGuest.currentGame

        assertNotNull(guestGame)
        assertTrue { guestGame.players[0].board.getPrisonYard(1,1)?.id == parentMale.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(2,1)?.id == parentFemale.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(3,2)?.id == tileRed.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(3,3)?.id == anotherTileRed.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(1,2)?.id == childToPlace?.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(2,2) is GuardTile }
    }

    /**
     * tests if preparing lists works properly on the network.
     **/
    @Test
    fun testPrepareLists() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        rootServiceHost.networkService.increasePrisoners(Triple(-100,-100, 0))
        rootServiceHost.networkService.increaseChildren(
            Triple(-100,-100, hostGame.allTiles[26] as PrisonerTile))
        rootServiceHost.networkService.increaseWorkers(Pair(-102,-102))
        rootServiceHost.networkService.increaseWorkers(Pair(-103,-103))
        rootServiceHost.networkService.increaseWorkers(Pair(-104,-104))
        rootServiceHost.networkService.increaseWorkers(Pair(20,30))

        val preparedLists = rootServiceHost.networkService.prepareLists()
        // animals
        assertTrue { preparedLists.first[0].x == 0 && preparedLists.first[0].y == 0 }
        assertTrue { preparedLists.first[0].truck == 0 }
        // children
        assertTrue { preparedLists.second[0].x == 0 && preparedLists.second[0].y == 0 }
        assertTrue { preparedLists.second[0].tileId == hostGame.allTiles[26].id }
        // worker
        assertTrue { preparedLists.third[0].x == 0 && preparedLists.third[0].y == 0 }
        assertTrue { preparedLists.third[0].jobEnum == JobEnum.MANAGER }
        assertTrue { preparedLists.third[1].x == 999 && preparedLists.third[1].y == 999 }
        assertTrue { preparedLists.third[1].jobEnum == JobEnum.CASHIER }
        assertTrue { preparedLists.third[2].x == 999 && preparedLists.third[2].y == 999 }
        assertTrue { preparedLists.third[2].jobEnum == JobEnum.KEEPER }
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
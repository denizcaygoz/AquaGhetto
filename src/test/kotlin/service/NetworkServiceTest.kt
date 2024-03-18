package tools.aqua.bgw.examples.war.service

import edu.udo.cs.sopra.ntf.AddTileToTruckMessage
import edu.udo.cs.sopra.ntf.BuyExpansionMessage
import edu.udo.cs.sopra.ntf.DiscardMessage
import edu.udo.cs.sopra.ntf.PositionPair
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
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

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
            PositionPair(4,7)))
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

    /**
     * Tests if moving tile works properly on the network.
     */
    @Test
    fun testMoveTile() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val isolationTile = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val parentFemale = PrisonerTile(25, PrisonerTrait.FEMALE, PrisonerType.BLUE)
        val tileRed = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val anotherTileRed = PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.RED)

        hostGame.players[0].coins = 20
        guestGame.players[0].coins = 20

        hostGame.players[1].coins = 20
        guestGame.players[1].coins = 20
        hostGame.players[1].isolation.add(parentFemale)
        guestGame.players[1].isolation.add(parentFemale)
        hostGame.players[1].isolation.add(isolationTile)
        guestGame.players[1].isolation.add(isolationTile)

        val successFirst = rootServiceHost.playerActionService.buyPrisonerFromOtherIsolation(
            hostGame.players[1], 1,1)

        if (successFirst.first) {
            rootServiceHost.networkService.sendMoveTile(hostGame.players[1].name, 1,1)
        }
        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostGame.players[0].board.getPrisonYard(1,1)?.id == isolationTile.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(1,1)?.id == isolationTile.id }
        /*change current player*/
        hostGame.currentPlayer = 1
        guestGame.currentPlayer = 1
        /*prepare yard of player two*/
        hostGame.players[1].board.setPrisonYard(1,1,isolationTile)
        guestGame.players[1].board.setPrisonYard(1,1,isolationTile)
        hostGame.players[1].board.setPrisonYard(3,2,tileRed)
        guestGame.players[1].board.setPrisonYard(3,2,tileRed)
        hostGame.players[1].board.setPrisonYard(3,3,anotherTileRed)
        guestGame.players[1].board.setPrisonYard(3,3,anotherTileRed)

        /** Setzt bei rootServiceHost komischer weiße die Eltern elemente auf not breedable **/
        val successSecond = rootServiceGuest.playerActionService.movePrisonerToPrisonYard(
             2,1)
        // Weil determineNextPlayer nicht richtig läuft
        rootServiceGuest.currentGame?.currentPlayer = 1

        val childToPlace: PrisonerTile? = successSecond.second
        if (successSecond.first && childToPlace!= null) {
            rootServiceGuest.playerActionService.placePrisoner(childToPlace, 1,2)
            /*must be done by the gui*/
            rootServiceGuest.networkService.increaseChildren(Triple(1,2,childToPlace))
            rootServiceGuest.playerActionService.moveEmployee(-101,-101, 2,2)
            /*must be done by the gui*/
            rootServiceGuest.networkService.increaseWorkers(Triple(2,2, GuardTile()))
            /*send message to player one*/
            /*must be sent by the gui*/
            rootServiceGuest.networkService.sendMoveTile(hostGame.players[1].name, 2,1)
        }

        rootServiceHost.waitForState(ConnectionState.PLAYING_MY_TURN)
        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_TURN)

        assertTrue { hostGame.players[1].board.getPrisonYard(1,1)?.id == isolationTile.id }
        assertTrue { hostGame.players[1].board.getPrisonYard(2,1)?.id == parentFemale.id }
        assertTrue { hostGame.players[1].board.getPrisonYard(3,2)?.id == tileRed.id }
        assertTrue { hostGame.players[1].board.getPrisonYard(3,3)?.id == anotherTileRed.id }
        //assertTrue { hostGame.players[1].board.getPrisonYard(1,2)?.id == childToPlace?.id }
    }

    /**
     * Tests if taking a truck works properly on the network.
     */
    @Test
    fun testTakeTruck() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

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
        //Weil DetermineNextPlayer falsch implementiert
        rootServiceHost.currentGame?.currentPlayer = 0
        //-----------------------------------------------

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
            rootServiceHost.networkService.increaseChildren(Triple(1,2, childToPlace))
        }
        /** place tile from bus and get worker **/
        rootServiceHost.playerActionService.placePrisoner(tileThree,3,2)
        //-----------------------------------------------
        rootServiceHost.networkService.increasePrisoners(Triple(3,2, 2))
        rootServiceHost.playerActionService.moveEmployee(-101,-101, 2,2)
        rootServiceHost.networkService.increaseWorkers(Triple(2,2, GuardTile()))
        /** send message to other player **/
        rootServiceHost.networkService.sendTakeTruck(0)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { guestGame.players[0].board.getPrisonYard(1,1)?.id == parentMale.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(2,1)?.id == parentFemale.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(3,2)?.id == tileRed.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(3,3)?.id == anotherTileRed.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(1,2)?.id == childToPlace?.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(2,2) is GuardTile }
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
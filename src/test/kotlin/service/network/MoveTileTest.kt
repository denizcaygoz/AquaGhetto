package service.network

import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import service.RootService
import service.networkService.ConnectionState
import kotlin.test.*

/**
 * [MoveTileTest] tests the GameActionMessage of moving a tile.
 **/
class MoveTileTest {
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
     * Tests if moving tile works properly on the network.
     */
    @Test
    fun testMoveTile() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        val guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val isolationTileHost = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val isolationTileGuest = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val parentFemaleHost = PrisonerTile(25, PrisonerTrait.FEMALE, PrisonerType.BLUE)
        val parentFemaleGuest = PrisonerTile(25, PrisonerTrait.FEMALE, PrisonerType.BLUE)
        val tileRed = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val anotherTileRed = PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.RED)

        hostGame.players[0].coins = 20
        guestGame.players[0].coins = 20

        hostGame.players[1].coins = 20
        guestGame.players[1].coins = 20
        hostGame.players[1].isolation.add(parentFemaleHost)
        guestGame.players[1].isolation.add(parentFemaleGuest)
        hostGame.players[1].isolation.add(isolationTileHost)
        guestGame.players[1].isolation.add(isolationTileGuest)

        rootServiceHost.playerActionService.buyPrisonerFromOtherIsolation( hostGame.players[1], 1,1)

        rootServiceHost.networkService.sendMoveTile(hostGame.players[1].name, 1,1)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { hostGame.players[0].board.getPrisonYard(1,1)?.id == isolationTileHost.id }
        assertTrue { guestGame.players[0].board.getPrisonYard(1,1)?.id == isolationTileHost.id }

        val gameAfterSafeHost = rootServiceHost.currentGame
        val gameAfterSafeGuest = rootServiceGuest.currentGame
        /*prepare yard of player two*/
        assertNotNull(gameAfterSafeGuest)
        assertNotNull(gameAfterSafeHost)
        gameAfterSafeHost.players[1].board.setPrisonYard(1,1,isolationTileHost)
        gameAfterSafeGuest.players[1].board.setPrisonYard(1,1,isolationTileGuest)
        gameAfterSafeHost.players[1].board.setPrisonYard(3,2,tileRed)
        gameAfterSafeGuest.players[1].board.setPrisonYard(3,2,tileRed)
        gameAfterSafeHost.players[1].board.setPrisonYard(3,3,anotherTileRed)
        gameAfterSafeGuest.players[1].board.setPrisonYard(3,3,anotherTileRed)

        val successSecond = rootServiceGuest.playerActionService.movePrisonerToPrisonYard(
            2,1)

        val childToPlace: PrisonerTile? = successSecond.second
        if (childToPlace!= null) {
            rootServiceGuest.playerActionService.placePrisoner(childToPlace, 1,2)
            /*must be done by the gui*/
            rootServiceGuest.playerActionService.moveEmployee(-101,-101, 2,2)
            /*send message to player one*/
            rootServiceGuest.networkService.sendMoveTile(hostGame.players[1].name, 2,1)
        }

        rootServiceGuest.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceHost.waitForState(ConnectionState.PLAYING_MY_TURN)

        assertTrue { gameAfterSafeHost.players[1].board.getPrisonYard(1,1)?.id == isolationTileGuest.id }
        assertTrue { gameAfterSafeHost.players[1].board.getPrisonYard(2,1)?.id == parentFemaleGuest.id }
        assertTrue { gameAfterSafeHost.players[1].board.getPrisonYard(3,2)?.id == tileRed.id }
        assertTrue { gameAfterSafeHost.players[1].board.getPrisonYard(3,3)?.id == anotherTileRed.id }
        // game changes parents to not breadable
        //assertTrue { hostGame.players[1].board.getPrisonYard(1,2)?.id == childToPlace?.id }
    }

    /**
     * Tests if moving tile resulting in placing a manager works properly on the network.
     */
    @Test
    fun testMoveTileManager() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        var guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val maleBlue = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val anotherMaleBlue = PrisonerTile(27, PrisonerTrait.MALE, PrisonerType.BLUE)
        val isolationTile = PrisonerTile(28, PrisonerTrait.OLD, PrisonerType.BLUE)
        val maleRed = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val anotherMaleRed = PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.RED)

        hostGame.players[0].coins = 20
        guestGame.players[0].coins = 20
        hostGame.players[0].isolation.add(isolationTile)
        guestGame.players[0].isolation.add(isolationTile)

        /*prepare yard of player two*/
        hostGame.players[0].board.setPrisonYard(1,1,maleBlue)
        guestGame.players[0].board.setPrisonYard(1,1,maleBlue)
        hostGame.players[0].board.setPrisonYard(1,2,anotherMaleBlue)
        guestGame.players[0].board.setPrisonYard(1,2,anotherMaleBlue)
        hostGame.players[0].board.setPrisonYard(3,2,anotherMaleRed)
        guestGame.players[0].board.setPrisonYard(3,2,anotherMaleRed)
        hostGame.players[0].board.setPrisonYard(3,3,maleRed)
        guestGame.players[0].board.setPrisonYard(3,3,maleRed)

        rootServiceHost.currentGame = hostGame

        rootServiceHost.playerActionService.movePrisonerToPrisonYard(2,1)
        rootServiceHost.currentGame?.currentPlayer = 0
        /*must be done by the gui*/
        rootServiceHost.playerActionService.moveEmployee(-101,-101, -102,-102)
        /*send message to player one*/
        /*must be sent by the gui*/
        rootServiceHost.currentGame?.currentPlayer = 0
        rootServiceHost.networkService.sendMoveTile(hostGame.players[0].name, 2,1)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        guestGame = rootServiceGuest.currentGame

        assertNotNull(guestGame)
        assertTrue { guestGame.players[0].hasJanitor }
    }

    /**
     * Tests if moving tile resulting in placing a cashier works properly on the network.
     */
    @Test
    fun testMoveTileCashier() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        var guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val maleBlue = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val anotherMaleBlue = PrisonerTile(27, PrisonerTrait.MALE, PrisonerType.BLUE)
        val isolationTile = PrisonerTile(28, PrisonerTrait.OLD, PrisonerType.BLUE)
        val maleRed = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val anotherMaleRed = PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.RED)

        hostGame.players[0].coins = 20
        guestGame.players[0].coins = 20
        hostGame.players[0].isolation.add(isolationTile)
        guestGame.players[0].isolation.add(isolationTile)

        /*prepare yard of player two*/
        hostGame.players[0].board.setPrisonYard(1,1,maleBlue)
        guestGame.players[0].board.setPrisonYard(1,1,maleBlue)
        hostGame.players[0].board.setPrisonYard(1,2,anotherMaleBlue)
        guestGame.players[0].board.setPrisonYard(1,2,anotherMaleBlue)
        hostGame.players[0].board.setPrisonYard(3,2,anotherMaleRed)
        guestGame.players[0].board.setPrisonYard(3,2,anotherMaleRed)
        hostGame.players[0].board.setPrisonYard(3,3,maleRed)
        guestGame.players[0].board.setPrisonYard(3,3,maleRed)

        rootServiceHost.currentGame = hostGame

        rootServiceHost.playerActionService.movePrisonerToPrisonYard(2,1)
        rootServiceHost.currentGame?.currentPlayer = 0
        /*must be done by the gui*/
        rootServiceHost.playerActionService.moveEmployee(-101,-101, -103,-103)
        /*send message to player one*/
        /*must be sent by the gui*/
        rootServiceHost.currentGame?.currentPlayer = 0
        rootServiceHost.networkService.sendMoveTile(hostGame.players[0].name, 2,1)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        guestGame = rootServiceGuest.currentGame

        assertNotNull(guestGame)
        assertTrue { guestGame.players[0].secretaryCount == 1 }
    }

    /**
     * Tests if moving tile resulting in placing a keeper works properly on the network.
     */
    @Test
    fun testMoveTileKeeper() {
        initConnections()

        val hostGame = rootServiceHost.currentGame
        var guestGame = rootServiceGuest.currentGame

        assertNotNull(hostGame)
        assertNotNull(guestGame)

        val maleBlue = PrisonerTile(26, PrisonerTrait.MALE, PrisonerType.BLUE)
        val anotherMaleBlue = PrisonerTile(27, PrisonerTrait.MALE, PrisonerType.BLUE)
        val isolationTile = PrisonerTile(28, PrisonerTrait.OLD, PrisonerType.BLUE)
        val maleRed = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val anotherMaleRed = PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.RED)

        hostGame.players[0].coins = 20
        guestGame.players[0].coins = 20
        hostGame.players[0].isolation.add(isolationTile)
        guestGame.players[0].isolation.add(isolationTile)

        /*prepare yard of player two*/
        hostGame.players[0].board.setPrisonYard(1,1,maleBlue)
        guestGame.players[0].board.setPrisonYard(1,1,maleBlue)
        hostGame.players[0].board.setPrisonYard(1,2,anotherMaleBlue)
        guestGame.players[0].board.setPrisonYard(1,2,anotherMaleBlue)
        hostGame.players[0].board.setPrisonYard(3,2,anotherMaleRed)
        guestGame.players[0].board.setPrisonYard(3,2,anotherMaleRed)
        hostGame.players[0].board.setPrisonYard(3,3,maleRed)
        guestGame.players[0].board.setPrisonYard(3,3,maleRed)

        rootServiceHost.currentGame = hostGame

        rootServiceHost.playerActionService.movePrisonerToPrisonYard(2,1)
        rootServiceHost.currentGame?.currentPlayer = 0
        /*must be done by the gui*/
        rootServiceHost.playerActionService.moveEmployee(-101,-101, -104,-104)
        /*send message to player one*/
        /*must be sent by the gui*/
        rootServiceHost.currentGame?.currentPlayer = 0
        rootServiceHost.networkService.sendMoveTile(hostGame.players[0].name, 2,1)

        rootServiceHost.waitForState(ConnectionState.WAITING_FOR_TURN)
        rootServiceGuest.waitForState(ConnectionState.PLAYING_MY_TURN)

        guestGame = rootServiceGuest.currentGame

        assertNotNull(guestGame)
        assertTrue { guestGame.players[0].lawyerCount == 1 }
    }
}
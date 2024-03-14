import entity.AquaGhetto
import entity.PrisonBus
import entity.tileTypes.*
import entity.*
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import org.junit.jupiter.api.assertThrows
import service.BoardService
import service.GameStatesService
import service.RootService
import java.util.Stack
import kotlin.test.BeforeTest
import kotlin.test.*

class BoardServiceTest {

    // Declaring necessary variables
    private lateinit var boardService: BoardService
    private lateinit var rootService: RootService
    private lateinit var gameStatesService: GameStatesService
    private var expected: AquaGhetto? = null

    // Setting up initial conditions before each test
    @BeforeTest
    fun setUp() {
        // Initializing services
        rootService = RootService()
        boardService = BoardService(rootService)
        // Creating player list for game initialization
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER),
            Pair("P3", PlayerType.PLAYER),
            Pair("P4", PlayerType.PLAYER)

        )
        // Starting a new game with the given players
        rootService.gameService.startNewGame(players)
        gameStatesService = GameStatesService(rootService)
    }

    // Test case for checking the creation of prison buses
    @Test
    fun testCreatePrisonBuses() {
        val buses = boardService.createPrisonBuses(3)
        assertEquals(3, buses.size)
        for (bus in buses) {
            assertEquals(0, bus.blockedSlots.count { it })
        }

        val buses2 = boardService.createPrisonBuses(2)
        assertEquals(3, buses2.size)
        assertEquals(0, buses2[0].blockedSlots.count { it })
        assertEquals(1, buses2[1].blockedSlots.count { it })
        assertEquals(2, buses2[2].blockedSlots.count { it })
    }

    // Test case for checking the creation of all tiles
    @Test
    fun testCreateAllTiles() {
        boardService.createAllTiles()
        val allTiles = rootService.currentGame!!.allTiles
        println(rootService.currentGame!!.players.size)
        assertEquals(114, allTiles.size)
    }

    // Test case for checking the retrieval of baby tile
    @Test
    fun getBabyTileTest(){
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
        val tile = rootService.boardService.getBabyTile(PrisonerType.RED)
        val tile2 = rootService.boardService.getBabyTile(PrisonerType.RED)
        assertEquals(tile.prisonerType, PrisonerType.RED)
        assertEquals(tile2.prisonerType, PrisonerType.RED)
        val tile3 = assertThrows<IllegalStateException> { rootService.boardService.getBabyTile(PrisonerType.RED) }
        assertEquals( "Found no baby card" , tile3.message)

    }

}

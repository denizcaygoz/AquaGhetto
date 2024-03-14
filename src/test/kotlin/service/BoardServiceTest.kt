import entity.enums.PlayerType
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import org.junit.jupiter.api.assertThrows
import service.BoardService
import service.GameStatesService
import service.RootService
import kotlin.test.BeforeTest
import kotlin.test.*

class BoardServiceTest {

    // Declaring necessary variables
    private lateinit var boardService: BoardService
    private lateinit var rootService: RootService
    private lateinit var gameStatesService: GameStatesService

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

    /**
     * Test if the right PrisonerTypes from the game are removed
     */
    @Test
    fun `test createDrawAndFinalStack`() {
        // Testing if the animal cards are missing
        val typesThatShouldBeDropped = listOf(
            PrisonerType.CYAN, PrisonerType.BROWN, PrisonerType.PURPLE
        )

        for (playerCount in 2..4) {
            val drawStacks = rootService.boardService.createStacks(playerCount)
            assertFalse(drawStacks.first.any {
                it is PrisonerTile && it.prisonerType in typesThatShouldBeDropped.take(5 - playerCount)
            })
            assertFalse(drawStacks.second.any {
                it is PrisonerTile && it.prisonerType in typesThatShouldBeDropped.take(5 - playerCount)
            })
        }
    }

    /**
     * Tests whether deck creation fails if an invalid player count was specified.
     */
    @Test
    fun `test createDrawAndFinalStack failing`() {
        val invalidPlayerCounts = listOf(0, 1, 6)
        for (count in invalidPlayerCounts) {
            val exception = assertThrows<IllegalStateException> { rootService.boardService.createStacks(count) }
            assertEquals("Not a valid amount of players.", exception.message)
        }
    }
}

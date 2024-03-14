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

    private lateinit var boardService: BoardService
    private lateinit var rootService: RootService
    private lateinit var gameStatesService: GameStatesService
    private var expected: AquaGhetto? = null

    @BeforeTest
    fun setUp() {
        rootService = RootService()
        boardService = BoardService(rootService)
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER),
            Pair("P3", PlayerType.PLAYER),
            Pair("P4", PlayerType.PLAYER)

        )
        rootService.gameService.startNewGame(players)
        gameStatesService = GameStatesService(rootService)
    }


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

    @Test
    fun testCreateAllTiles() {
        boardService.createAllTiles()
        val allTiles = rootService.currentGame!!.allTiles
        println(rootService.currentGame!!.players.size)
        assertEquals(114, allTiles.size)
    }

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

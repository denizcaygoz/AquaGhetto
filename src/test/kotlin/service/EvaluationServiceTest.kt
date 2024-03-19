package service

import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test class for evaluating the functionalities of EvaluationService.
 */
class EvaluationServiceTest {
    private val testRefreshable = TestRefreshable()
    private val rootService = RootService()

    /**
     * Sets up a mock game for testing.
     */
    @BeforeTest
    fun setUpMockGame(){
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        testRefreshable.reset()
        rootService.gameService.startNewGame(players)
        rootService.addRefreshable(testRefreshable)
    }

    /**
     * Tests the count of different prisoner types.
     */
    @Test
    fun getPrisonerCountTypeTest(){
        val game = rootService.currentGame!!
        // Expected map containing counts of different prisoner types
        val map1 = mutableMapOf(PrisonerType.PURPLE to 1, PrisonerType.RED to 2)

        // Setting up prisoner tiles on the board for testing
        game.players.first().board.setPrisonYard(10,10, PrisonerTile(0, PrisonerTrait.RICH, PrisonerType.PURPLE))
        game.players.first().board.setPrisonYard(20,20, PrisonerTile(1, PrisonerTrait.RICH, PrisonerType.RED))
        game.players.first().board.setPrisonYard(30,30, PrisonerTile(2, PrisonerTrait.RICH, PrisonerType.RED))
        // Asserting that the count of prisoner types matches the expected map
        assertEquals(map1, rootService.evaluationService.getPrisonerTypeCount(game.players.first()))
    }

    /**
     * Tests whether the game evaluates the boards correctly and applies all
     * possible bonus points and minuspoints.
     */
    @Test
    fun `test evaluateGame and evaluatePlayer`() {
        val game = rootService.currentGame!!

        // Setting up a mock evaluation
        val prisonerTileToPlace = PrisonerTile(
            99, PrisonerTrait.NONE, PrisonerType.RED
        )
        game.players.forEach {
            for (x in -1..1) {
                for (y in -1..1) {
                    it.board.setPrisonYard(2 + x, 2 + y, prisonerTileToPlace)
                }
            }

            for (type in PrisonerType.values()) {
                it.isolation.push(PrisonerTile(99, PrisonerTrait.NONE, type))
            }
        }

        // Second player gets all the employees
        game.players[1].board.setPrisonYard(2, 1, PrisonerTile(
            99, PrisonerTrait.RICH, PrisonerType.RED
        ))
        game.players[1].board.setPrisonYard(2, 2, GuardTile())
        game.players[1].board.guardPosition.add(Pair(2, 2))
        game.players[1].hasJanitor = true
        game.players[1].secretaryCount += 2
        game.players[1].lawyerCount += 2
        game.players[1].coins = 1

        rootService.evaluationService.evaluateGame()

        assertEquals(-7, game.players[0].currentScore)
        assertEquals(12, game.players[1].currentScore)
        assertTrue(testRefreshable.refreshAfterEndGameCalled)
    }

}
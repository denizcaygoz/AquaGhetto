package service

import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import kotlin.test.Test
import kotlin.test.assertEquals

// Test class for evaluating the functionalities of EvaluationService
class EvaluationServiceTest {
    // List of players used for testing
    val players = mutableListOf(
        Pair("P1", PlayerType.PLAYER),
        Pair("P2", PlayerType.PLAYER)
    )
    // RootService instance for testing
    val rootService = RootService()
    // Test method for checking the count of different prisoner types
    @Test
    fun getPrisonerCountTypeTest(){
        // Expected map containing counts of different prisoner types
        val map1 = mutableMapOf(PrisonerType.PURPLE to 1, PrisonerType.RED to 2)
        // Starting a new game with specified players
        rootService.gameService.startNewGame(players)
        // Setting up prisoner tiles on the board for testing
        rootService.currentGame!!.players.first().board.setPrisonYard(10,10, PrisonerTile(0, PrisonerTrait.RICH, PrisonerType.PURPLE))
        rootService.currentGame!!.players.first().board.setPrisonYard(20,20, PrisonerTile(1, PrisonerTrait.RICH, PrisonerType.RED))
        rootService.currentGame!!.players.first().board.setPrisonYard(30,30, PrisonerTile(2, PrisonerTrait.RICH, PrisonerType.RED))
        // Asserting that the count of prisoner types matches the expected map
        assertEquals(map1, rootService.evaluationService.getPrisonerTypeCount(rootService.currentGame!!.players.first()))
    }

}
package service.ai

import entity.enums.PlayerType
import org.junit.jupiter.api.assertDoesNotThrow
import service.RootService
import kotlin.test.Test

/**
 * Test class for evaluating basic functionalities of Smart AI.
 */
class SmartAIBasicTest {
    /**
     * Test for ensuring no errors occur during AI initialization.
     */
    @Test
    fun testForError() {
        val rootService = RootService()
        val players = mutableListOf(
            Pair("P1", PlayerType.RANDOM_AI),
            Pair("P2", PlayerType.AI)
        )

        assertDoesNotThrow { rootService.gameService.startNewGame(players) }
        val game = rootService.currentGame
        require(game != null)
        println("Last DrawStack: ${game.drawStack.size}    FinalStack: ${game.finalStack.size}")
    }


}
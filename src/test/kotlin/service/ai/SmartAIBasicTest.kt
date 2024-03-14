package service.ai

import entity.AquaGhetto
import entity.enums.PlayerType
import service.RootService
import kotlin.concurrent.thread
import kotlin.test.Test

class SmartAIBasicTest {
    private val rootService = RootService()

    @Test
    fun testForError() {

        val players = mutableListOf(
            Pair("P1", PlayerType.AI),
            Pair("P2", PlayerType.AI)
        )

        rootService.gameService.startNewGame(players)
        val game = rootService.currentGame
        require(game != null)
        println("Last DrawStack: ${game.drawStack.size}    FinalStack: ${game.finalStack.size}")
    }





    private fun createBasicGame(): AquaGhetto {
        val players = mutableListOf(
            Pair("P1", PlayerType.AI),
            Pair("P2", PlayerType.AI)
        )
        rootService.gameService.startNewGame(players)
        val game = rootService.currentGame
        require(game != null)
        return game
    }

}
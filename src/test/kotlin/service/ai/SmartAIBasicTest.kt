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
        printStackSize()
        rootService.gameService.startNewGame(players)
        val game = rootService.currentGame
        require(game != null)

    }

    fun printStackSize() {
        thread {
            println("ABC")
            while (true) {
                //println("1")
                Thread.sleep(1)
                //println("2")
                val game = rootService.currentGame ?: continue
                //println("3")
                println("DrawStack: ${game.drawStack.size}    FinalStack: ${game.finalStack.size}")
                //println("4")
            }
        }
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
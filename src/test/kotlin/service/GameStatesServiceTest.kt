import org.junit.jupiter.api.Test
import kotlin.test.*
import entity.enums.PlayerType
import org.junit.jupiter.api.assertThrows
import service.*
import java.io.File

class GameStatesServiceTest {
    val rootService = RootService()
        @Test
        fun loadInvalidGameTest(){
            // Test when no saved file exists
            val saveFile = File("saveFile")
            if (saveFile.exists()) saveFile.delete()
            val game1 = assertThrows<IllegalStateException> { rootService.gameStatesService.loadGame() }
            assertEquals( "There is no game to load", game1.message)
        }

    //TODO loadValidGameTest

    @Test
    fun saveInvalidGameTest(){
        // Test when no saved file exists
        val message1 = assertThrows<IllegalStateException> { rootService.gameStatesService.saveGame() }
        assertEquals( "No game to save", message1.message)
    }

    //TODO saveValidGameTest

    @Test
    fun undoInvalidGameState(){
        val players = mutableListOf(
                Pair("P1", PlayerType.PLAYER),
                Pair("P2", PlayerType.PLAYER)
            )
            rootService.gameService.startNewGame(players)
        val message1 = assertThrows<IllegalStateException> { rootService.gameStatesService.undo() }
        assertEquals( "Nothing to undo" , message1.message)
    }

    @Test
    fun undoValidGameState() {
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
        val firstState = rootService.currentGame
        firstState!!.players[firstState.currentPlayer] = firstState.players.last()

        firstState.previousState = firstState

        rootService.gameStatesService.undo()

        assertNotNull(rootService.currentGame!!.previousState)
        assertEquals(
            rootService.currentGame!!.players.last(),
            rootService.currentGame!!.players[rootService.currentGame!!.currentPlayer]
        )

    }

    @Test
    fun redoInvalidGameState(){
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
        val message1 = assertThrows<IllegalStateException> { rootService.gameStatesService.redo() }
        assertEquals( "Nothing to redo" , message1.message)
    }

    @Test
    fun redoValidGameState() {
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
        val firstState = rootService.currentGame
        firstState!!.players[firstState.currentPlayer] = firstState.players.last()

        firstState.nextState = firstState

        rootService.gameStatesService.redo()

        assertNotNull(rootService.currentGame!!.nextState)
        assertEquals(
            rootService.currentGame!!.players.last(),
            rootService.currentGame!!.players[rootService.currentGame!!.currentPlayer]
        )

    }



}
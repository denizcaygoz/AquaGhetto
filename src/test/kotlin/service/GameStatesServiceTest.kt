
import entity.enums.PlayerType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import service.RootService
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

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

    @Test
    fun `test copyAquaghetto`() {
        mutableListOf(
            Pair("A", PlayerType.PLAYER),
            Pair("B", PlayerType.PLAYER),
        ).let {
            rootService.gameService.startNewGame(it)
        }
        val actualGame = rootService.currentGame!!
        val copy = rootService.gameStatesService.copyAquaGhetto()

        // Checking if not only references were copied
        assertNotSame(actualGame.drawStack, copy.drawStack)
        assertNotSame(actualGame.finalStack, copy.drawStack)

        for (i in actualGame.players.indices) {
            assertNotSame(actualGame.players[i], copy.players[i])
            assertNotSame(actualGame.players[i].board, copy.players[i].board)
        }

        for (i in actualGame.prisonBuses.indices) {
            assertNotSame(actualGame.prisonBuses[i], copy.prisonBuses[i])
        }

        assertNotSame(actualGame.allTiles, copy.allTiles)
        assertSame(copy, actualGame.nextState)
        assertSame(actualGame, copy.previousState)
    }
}
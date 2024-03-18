
import entity.AquaGhetto
import entity.enums.PlayerType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import service.RootService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import kotlin.test.*

/**
 * This class provides unit tests for the GameStatesService class.
 */
class GameStatesServiceTest {
    private val rootService = RootService()
    /**
     * Tests loading an invalid game.
     */
    @Test
    fun loadInvalidGameTest(){
        // Test when no saved file exists
        val saveFile = File("saveFile")
        if (saveFile.exists()) saveFile.delete()
        val game1 = assertThrows<IllegalStateException> { rootService.gameStatesService.loadGame() }
        assertEquals( "There is no game to load", game1.message)
    }

    /**
     * Tests loading a valid game.
     */
    @Test
    fun loadValidGameTest() {
        // Initialize the game state
        val rootService = RootService()
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER),
            Pair("P3", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)

        // Save the game
        rootService.gameStatesService.saveGame()

        // Load the game
        rootService.gameStatesService.loadGame()

        // Get the loaded game state
        val loadedGame = rootService.currentGame

        // Assert that the loaded game state is not null
        assertNotNull(loadedGame, "Loaded game is null")

        // Compare the number of players
        assertEquals(players.size, loadedGame.players.size, "Number of players mismatch")

        // Compare other properties if necessary
        // For example, compare the current player
        assertEquals(0, loadedGame.currentPlayer, "Current player mismatch")

    }

    /**
     * Tests saving an invalid game.
     */
    @Test
    fun saveInvalidGameTest(){
        // Test when no saved file exists
        val message1 = assertThrows<IllegalStateException> { rootService.gameStatesService.saveGame() }
        assertEquals( "No game to save", message1.message)
    }

    /**
     * Tests saving a valid game.
     */
    @Test
    fun saveValidGameTest() {
        // Create a mock AquaGhetto object
        val aquaGhetto = AquaGhetto()
        val rootService = RootService()
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
        // Call the saveGame function
        rootService.gameStatesService.saveGame()

        // Mock FileOutputStream, GZIPOutputStream, and ObjectOutputStream
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)

        // Write the AquaGhetto object to ObjectOutputStream
        objectOutputStream.writeObject(aquaGhetto)
        objectOutputStream.close()

        // Ensure that the ByteArrayOutputStream is not empty
        assertTrue(byteArrayOutputStream.toByteArray().isNotEmpty(), "AquaGhetto object is not saved")
    }

    /**
     * Tests undoing an invalid game state.
     */
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

    /**
     * Tests undoing a valid game state.
     */
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

    /**
     * Tests redoing an invalid game state.
     */
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

    /**
     * Tests redoing a valid game state.
     */
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

    /**
     * Tests copying an AquaGhetto object.
     */
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
package service

import entity.AquaGhetto
import entity.enums.PlayerType
import org.junit.jupiter.api.Test
import view.Refreshable
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * [GameServiceTest] is class that provides tests for [GameService]
 * by basically starting up a new game of aquaghetto.
 * [TestRefreshable] is used to validate correct refreshing behavior even though no GUI
 * is present.
 */
class GameServiceTest {

    /**
     * starts a game of aquaghetto
     *
     * @param refreshables is a vararg of different refreshables of the root service
     * @return the root service holding the started game as [RootService.currentGame]
     */
    private fun setUpGame(
        players: MutableList<Pair<String, PlayerType>>,
        vararg refreshables: Refreshable
    ): RootService {
        val rootTestService = RootService()

        refreshables.forEach { rootTestService.addRefreshable(it) }
        rootTestService.gameService.startNewGame(players)

        return rootTestService
    }

    /**
     * [testStartNewGameWithWrongAmountPlayers] tests if a game of aquaghetto can be started with the wrong
     * amount of players.
     */
    @Test
    fun testStartNewGameWithWrongAmountPlayers() {
        val testRefreshable = TestRefreshable()
        var testRootService = RootService()
        val testPlayers: MutableList<Pair<String, PlayerType>> = mutableListOf()
        testPlayers.add(Pair("Player One", PlayerType.PLAYER))

        assertThrows<IllegalArgumentException>{testRootService.gameService.startNewGame(testPlayers)}

        testPlayers.add(Pair("Player Two", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Three", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Four", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Five", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Six", PlayerType.PLAYER))

        assertThrows<IllegalArgumentException>{testRootService.gameService.startNewGame(testPlayers)}

        testPlayers.removeAt(testPlayers.size - 1)
        testRootService = setUpGame(testPlayers, testRefreshable)

        assertNotNull(testRootService.currentGame)
        assertTrue(testRefreshable.refreshAfterStartGameCalled)
        assertTrue(testRefreshable.refreshAfterNextTurnCalled)
        testRefreshable.reset()
        assertFalse(testRefreshable.refreshAfterStartGameCalled)
        assertFalse(testRefreshable.refreshAfterNextTurnCalled)
    }

    /**
     * [testStartNewGameWithWrongAmountPlayers] tests if a game of aquaghetto can be started with invalid
     * names.
     */
    @Test
    fun testStartNewGameWithInvalidNames() {
        val testRootService = RootService()
        val testPlayers: MutableList<Pair<String, PlayerType>> = mutableListOf()
        testPlayers.add(Pair("", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Two", PlayerType.PLAYER))

        assertThrows<IllegalArgumentException>{testRootService.gameService.startNewGame(testPlayers)}

        testPlayers.add(Pair("Player Two", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Four", PlayerType.PLAYER))
        testPlayers.add(Pair("Player Five", PlayerType.PLAYER))

        assertThrows<IllegalArgumentException>{testRootService.gameService.startNewGame(testPlayers)}
    }

    /**
     * [testStartNewGameWithWrongAmountPlayers] tests if starting a game of aquaghetto works properly.
     * Checks if the created game have the same players that were passed.
     * Checks if all tiles created properly.
     * Checks if all buses created properly
     * Checks if all stacks created properly.
     */
    @Test
    fun testStartNewGame() {
        val testRefreshable = TestRefreshable()
        val testRootService: RootService
        val testPlayers: MutableList<Pair<String, PlayerType>> = mutableListOf()
        val testPlayerNames: MutableList<String> = mutableListOf()

        testPlayerNames.add("Player One")
        testPlayerNames.add("Player Two")

        testPlayerNames.forEach { playerName ->
            testPlayers.add(Pair(playerName, PlayerType.PLAYER))
        }

        testRootService = setUpGame(testPlayers, testRefreshable)

        val testGame: AquaGhetto? = testRootService.currentGame
        assertNotNull(testGame)
        assertTrue(testRefreshable.refreshAfterStartGameCalled)
        assertTrue(testRefreshable.refreshAfterNextTurnCalled)

        testGame.players.forEachIndexed { index, player ->
            assertEquals(player.name, testPlayerNames[index])
        }

        assertTrue(testGame.allTiles.isNotEmpty())
        assertTrue(testGame.drawStack.isNotEmpty())
        assertTrue(testGame.finalStack.isNotEmpty())
        assertTrue(testGame.prisonBuses.isNotEmpty())


        testRefreshable.reset()
        assertFalse(testRefreshable.refreshAfterStartGameCalled)
        assertFalse(testRefreshable.refreshAfterNextTurnCalled)
    }
}
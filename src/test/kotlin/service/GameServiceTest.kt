package service

import entity.AquaGhetto
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import view.Refreshable
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

    /**
     * Tests if the turn is passed correctly and if the same player stays the current player
     * when the other player has already taken a bus.
     */
    @Test
    fun `test determineNextPlayer for two players`() {
        val rs = RootService()
        val players = mutableListOf(
            Pair("One", PlayerType.PLAYER),
            Pair("Two", PlayerType.PLAYER)
        )
        rs.gameService.startNewGame(players)

        // Placing a tile
        rs.playerActionService.addTileToPrisonBus(
            PrisonerTile(1, PrisonerTrait.NONE, PrisonerType.RED),
            rs.currentGame!!.prisonBuses[0]
        )
        assertEquals(1, rs.currentGame!!.currentPlayer)

        // placing another tile
        rs.playerActionService.addTileToPrisonBus(
            PrisonerTile(3, PrisonerTrait.NONE, PrisonerType.RED),
            rs.currentGame!!.prisonBuses[1]
        )
        assertEquals(0, rs.currentGame!!.currentPlayer)

        // Taking a bus and withdrawing
        rs.playerActionService.takePrisonBus(rs.currentGame!!.prisonBuses[0])
        assertEquals(0, rs.currentGame!!.currentPlayer)
        rs.gameService.determineNextPlayer(true)

        // Placing yet another tile, player should stay the same
        rs.playerActionService.addTileToPrisonBus(
            PrisonerTile(3, PrisonerTrait.NONE, PrisonerType.RED),
            rs.currentGame!!.prisonBuses[0]
        )
        assertEquals(1, rs.currentGame!!.currentPlayer)

        // New Round, player should stay the same
        rs.playerActionService.takePrisonBus(rs.currentGame!!.prisonBuses[0])
        assertEquals(1, rs.currentGame!!.currentPlayer)
    }

    /**
     * Tests if current player rotates correctly and if players who have already taken a bus
     * gets skipped.
     */
    @Test
    fun `test determineNextPlayer for four players`() {
        val rs = RootService()
        val players = mutableListOf(
            Pair("One", PlayerType.PLAYER),
            Pair("Two", PlayerType.PLAYER),
            Pair("Three", PlayerType.PLAYER),
            Pair("Four", PlayerType.PLAYER),
        )
        rs.gameService.startNewGame(players)

        // Every player adds a tile, should end up at starting player
        rs.currentGame!!.prisonBuses.forEach {
            rs.playerActionService.addTileToPrisonBus(
                CoinTile(10), it
            )
        }
        assertEquals(0, rs.currentGame!!.currentPlayer)

        // Starting player takes a bus, ends turn, next player should have their turn now
        rs.playerActionService.takePrisonBus(rs.currentGame!!.prisonBuses[0])
        assertEquals(0, rs.currentGame!!.currentPlayer)
        rs.gameService.determineNextPlayer(true)

        // Everyone places again, should end up at player 2 again
        for (i in 0..2) {
            rs.playerActionService.addTileToPrisonBus(
                CoinTile(10), rs.currentGame!!.prisonBuses[i]
            )
        }
        assertEquals(1, rs.currentGame!!.currentPlayer)
    }
}
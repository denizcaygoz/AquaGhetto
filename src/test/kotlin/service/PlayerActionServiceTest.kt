package service

import entity.enums.PlayerType
import kotlin.test.*

class PlayerActionServiceTest {
    private val rootService = RootService()
    @BeforeTest
    fun setUpMockGame() {
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
    }

    @Test
    fun `test expandPrisonGrid for a big Extension`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]

        // Test big Expansion:
        val bigExpX = 5
        val bigExpY = 2
        currentPlayer.coins = 2
        assert(rootService.validationService.validateExpandPrisonGrid(true, bigExpX, bigExpY, 0))
        rootService.playerActionService.expandPrisonGrid(true, bigExpX, bigExpY, 0)

        // Testing correct resource deduction
        assertSame(0, currentPlayer.coins)
        assertSame(1, currentPlayer.remainingBigExtensions)
    }

    @Test
    fun `test expandPrisonGrid for a small extension`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]

        // Test invalid location
        val smallExpX = 2
        val smallExpY = 2
        currentPlayer.coins = 1

        val rotations = listOf(0, 90, 180, 270)

        // These placements should fail, dead center of the board:
        rotations.forEach {
            assert(!rootService.validationService.validateExpandPrisonGrid(false, smallExpX, smallExpY, it))
            assertFails { rootService.playerActionService.expandPrisonGrid(false, smallExpX, smallExpY, it) }
        }
        assertSame(1, currentPlayer.coins)
        assertSame(2, currentPlayer.remainingSmallExtensions)

        // These placements should all succeed:
        val validLocations = listOf(
            Triple(0, 1, 0),
            Triple(6, 3, 90),
            Triple(4, 5, 180),
            Triple(4, 0, 270),
        )

        validLocations.forEach {
            val xPos = it.first
            val yPos = it.second
            val rotation = it.third

            currentPlayer.remainingSmallExtensions++
            currentPlayer.coins++
            assert(rootService.validationService.validateExpandPrisonGrid(false, xPos, yPos, rotation)) {
                "($xPos, $yPos) is not a valid location for $rotation"
            }
            rootService.playerActionService.expandPrisonGrid(false, xPos, yPos, rotation)
        }
        assertSame(1,currentPlayer.coins)
        assertSame(2, currentPlayer.remainingSmallExtensions)
    }
}
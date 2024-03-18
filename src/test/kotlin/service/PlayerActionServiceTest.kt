package service

import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.*

class PlayerActionServiceTest {
    private val rootService = RootService()
    private val testRefreshable = TestRefreshable()
    @BeforeTest
    fun setUpMockGame() {
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        testRefreshable.reset()
        rootService.gameService.startNewGame(players)
        rootService.addRefreshable(testRefreshable)
    }

    /**
     * Tests whether a PrisonerTile can be placed on valid locations
     * and not placed on invalid locations
     */
    @Test
    fun `test placePrisoner on (in)valid grid postion`() {
        val tileToPlace = PrisonerTile(
            11, PrisonerTrait.FEMALE, PrisonerType.RED
        )

        // Testing if placed on valid location
        val tilePlaced = rootService.playerActionService.placePrisoner(
            tileToPlace, x = 2, y = 2
        )

        assert(tilePlaced.first) { "Placing a Tile did not succeed" }
        assertNull(tilePlaced.second)
        assert(testRefreshable.refreshScoreStatsCalled)
        assert(testRefreshable.refreshPrisonCalled)

        // Testing if placing on wrong postion:
        val invalidTilePlaced = rootService.playerActionService.placePrisoner(
            tileToPlace, x = 100, y = 100
        )
        assert(!invalidTilePlaced.first)
        assertNull(invalidTilePlaced.second)
    }

    /**
     * Tests whether placePrisoner gives out boni if applicable.
     */
    @Test
    fun `test if placePrisoner gives out boni`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]

        val malePrisoner = PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED)
        val femalePrisoner = PrisonerTile(11, PrisonerTrait.FEMALE, PrisonerType.RED)

        val maleResult = rootService.playerActionService.placePrisoner(malePrisoner, 2, 2, changePlayer = false)
        val femaleResult = rootService.playerActionService.placePrisoner(femalePrisoner, 3, 2, changePlayer = false)

        // First placement should result in no child
        assert(maleResult.first)
        assertNull(maleResult.second)

        // Second placement should result in a child of a red prisoner
        assert(femaleResult.first)
        assertSame(PrisonerTrait.BABY, femaleResult.second?.prisonerTrait)
        assert(femaleResult.second?.id in setOf(22, 23))

        // both parents should be infertile now
        assert(!malePrisoner.breedable)
        assert(!femalePrisoner.breedable)

        // Placing the baby should also yield a coin bonus
        val oldCoins = currentPlayer.coins
        val babyResult = rootService.playerActionService.placePrisoner(
            femaleResult.second!!, 2, 3, changePlayer = false
        )
        assert(babyResult.first)
        assertNull(babyResult.second)
        assertSame(oldCoins + 1, currentPlayer.coins)

        // Placing two more tiles for the employee bonus:
        val firstTile = PrisonerTile(15, PrisonerTrait.NONE, PrisonerType.RED)
        val secondTile = PrisonerTile(16, PrisonerTrait.NONE, PrisonerType.RED)
        rootService.playerActionService.placePrisoner(firstTile, 4, 2, changePlayer = false)
        rootService.playerActionService.placePrisoner(secondTile, 4, 3, changePlayer = false)
        assert(currentPlayer.board.getPrisonYard(-101, -101) is GuardTile)
    }

    @Test
    fun `test expandPrisonGrid for a big Extension`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]

        // Test big Expansion:
        var bigExpX = 5
        var bigExpY = 2
        currentPlayer.coins = 2
        assert(rootService.validationService.validateExpandPrisonGrid(true, bigExpX, bigExpY, 0))
        rootService.playerActionService.expandPrisonGrid(true, bigExpX, bigExpY, 0)

        // Testing correct resource deduction
        assertSame(0, currentPlayer.coins)
        assertSame(1, currentPlayer.remainingBigExtensions)

        // Testing failure if not adjacent to any grid
        bigExpX = 7
        bigExpY = 7
        currentPlayer.coins = 2
        assert(!rootService.validationService.validateExpandPrisonGrid(true, bigExpX, bigExpY, 0))
        assertFails { rootService.playerActionService.expandPrisonGrid(true, bigExpX, bigExpY, 0) }
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
            assertFails {
                rootService.playerActionService.expandPrisonGrid(
                    false, smallExpX, smallExpY, it, changePlayer = false
                )
            }
        }
        assertSame(1, currentPlayer.coins)
        assertSame(2, currentPlayer.remainingSmallExtensions)

        // These placements should all succeed:
        val validLocations = listOf(
            Triple(1, 6, 0),
            Triple(6, 3, 90),
            Triple(4, 4, 180),
            Triple(4, 0, 270),
        )

        validLocations.forEach {
            val xPos = it.first
            val yPos = it.second
            val rotation = it.third

            currentPlayer.remainingSmallExtensions++
            currentPlayer.coins++
            assert(rootService.validationService.validateExpandPrisonGrid(false, xPos, yPos, rotation)) {
                "($xPos, $yPos) is not a valid location for $rotation°"
            }
            rootService.playerActionService.expandPrisonGrid(
                false, xPos, yPos, rotation, changePlayer = false
            )
        }
        assertSame(1,currentPlayer.coins)
        assertSame(2, currentPlayer.remainingSmallExtensions)
    }

    @Test
    fun `move employee from source to destination within prison yard`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.coins = 1
        currentPlayer.board.setPrisonYard(1, 1, GuardTile()) // Place a guard tile at (1,1)

        rootService.playerActionService.moveEmployee(1,1,2,2)

        assertTrue(currentPlayer.board.getPrisonYard(2, 2) is GuardTile)
        assertEquals(0, currentPlayer.coins) // Check coins decremented
    }

    @Test
    fun `move notGuard from source to empty destination`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.hasJanitor = true
        currentPlayer.coins = 1 // Set up a janitor

        rootService.playerActionService.moveEmployee(-102, -102, 3, 3)

        assertFalse(currentPlayer.hasJanitor)
        assertEquals(0, currentPlayer.coins) // Check coins decremented
    }

    @Test
    fun `move Guard to special destination`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.board.setPrisonYard(3, 3, GuardTile()) // Place a guard tile at (3,3)
        currentPlayer.coins = 1 // Set up coins for placement

        rootService.playerActionService.moveEmployee(3, 3, -103, -103)

        assertFalse(currentPlayer.board.getPrisonYard(3, 3) is GuardTile)
        assertTrue(currentPlayer.secretaryCount == 1) // Check janitor removed
        assertEquals(0, currentPlayer.coins) // Check coins decremented
    }
    @Test
    fun `move employee to special destination without enough coins`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.board.setPrisonYard(3, 3, GuardTile()) // Place a guard tile at (3,3)
        currentPlayer.coins = 0

        assertFails{rootService.playerActionService.moveEmployee(3, 3, -103, -103)}
    }

    @Test
    fun `move employee to full destination`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.secretaryCount = 2 // Maximum secretaries already
        currentPlayer.coins = 1

        assertFails{rootService.playerActionService.moveEmployee(3, 3, -103, -103)}
    }

    /**
     * Tests a valid purchase from one player's isolation.
     */
    @Test
    fun `buy Prisoner from valid isolation to valid location`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.coins += 2

        val prisonerTile = PrisonerTile(3, PrisonerTrait.NONE, PrisonerType.RED)

        val playerToBuyFrom = game.players[game.currentPlayer + 1]

        playerToBuyFrom.isolation.push(prisonerTile)

        val oldCoins = playerToBuyFrom.coins
        val oldCoinsCurrentPlayer = currentPlayer.coins
        val placementResult = rootService.playerActionService.buyPrisonerFromOtherIsolation(playerToBuyFrom, 2, 2)

        assertTrue { testRefreshable.refreshScoreStatsCalled }
        assertTrue { testRefreshable.refreshPrisonCalled }
        assertTrue { testRefreshable.refreshIsolationCalled }

        assertFails { playerToBuyFrom.isolation.peek() }
        assertEquals(oldCoinsCurrentPlayer - 2, currentPlayer.coins)
        assertEquals(oldCoins + 1, playerToBuyFrom.coins)
        assertTrue(placementResult.first)
        assertNull(placementResult.second)
    }

    /**
     * Tests all cases where a purchase can fail or is invalid.
     */
    @Test
    fun `buy Prisoner failing`() {
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        val playerToBuyFrom = game.players[game.currentPlayer + 1]
        val prisonerTile = PrisonerTile(3, PrisonerTrait.NONE, PrisonerType.RED)

        // Should fail because not enough coins
        assertFails { rootService.playerActionService.buyPrisonerFromOtherIsolation(playerToBuyFrom, 2, 2) }
        currentPlayer.coins += 2

        // Should fail because empty isolation
        assertFails { rootService.playerActionService.buyPrisonerFromOtherIsolation(playerToBuyFrom, 2, 2) }
        playerToBuyFrom.isolation.push(prisonerTile)

        // Should fail because selectedPlayer == currentPlayer
        assertFails { rootService.playerActionService.buyPrisonerFromOtherIsolation(currentPlayer, 2, 2) }

        // Should fail because invalid location
        val result = rootService.playerActionService.buyPrisonerFromOtherIsolation(playerToBuyFrom, 2, 69)

        assertFalse(result.first)
        assertNull(result.second)
    }


    /**
     * This function tests to take a bus from the middle successfully by a player.
     */
    @Test
    fun `test attempting to take a bus successfully`() {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentPlayer = game.players[game.currentPlayer]

        //initial prison bus number are saved.
        val prisonBuses = game.prisonBuses.size
        //Player should not have a bus since he hasn't taken a bus yet.
        assertEquals(null,currentPlayer.takenBus, "In the inital state Player do not have a Bus.")

        val tile = game.drawStack.pop()
        //saving a tile to prison bus the player want to take.
        game.prisonBuses[0].tiles[2] = tile
        //take prison bus action is executed.
        rootService.playerActionService.takePrisonBus(game.prisonBuses[0])
        //Busses on the mid should be decremented by 1.
        assertEquals(prisonBuses-1,game.prisonBuses.size,"1 Bus taken from the mid by Player.")
        //we're excepting that player takes a bus.
        checkNotNull(currentPlayer.takenBus)
    }


    /**
     * This function tests player trying to take a bus when there is no bus on the mid.
     */
    @Test
    fun `test attempting to take a bus when there are no buses available`() {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentPlayer = game.players[game.currentPlayer]

        //to clear prison busses on the mid.
        game.prisonBuses.clear()

        //Player should not have a bus since he hasn't taken a bus yet.
        assertEquals(null,currentPlayer.takenBus, "In the inital state Player do not have a Bus.")

        assertFailsWith<IndexOutOfBoundsException> {
            rootService.playerActionService.takePrisonBus(game.prisonBuses[0])
        }
    }

    /**
     * tests to take a bus when there is no tile on it.
     */
    @Test
    fun `test attempting to take a bus with no tiles`() {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentPlayer = game.players[game.currentPlayer]

        assertEquals(null,currentPlayer.takenBus, "In the inital state Player do not have a Bus.")

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.takePrisonBus(game.prisonBuses[0])
        }
    }

    /**
     * tests the Player taking a bus while he has already a one.
     */
    @Test
    fun `test attempting to take a bus when already having one`() {
        val game = rootService.currentGame
        checkNotNull(game)
        val currentPlayer = game.players[game.currentPlayer]

        //assigning the second bus as player's bus.
        currentPlayer.takenBus = game.prisonBuses[1]

        //Player should have a bus.
        checkNotNull(currentPlayer.takenBus)

        val tile = game.drawStack.pop()
        //saving a tile to prison bus the player want to take.
        game.prisonBuses[0].tiles[2] = tile

        assertFailsWith<java.lang.IllegalArgumentException> {
            rootService.playerActionService.takePrisonBus(game.prisonBuses[0])
        }
    }
    /**
     * tests if player can add a tile to the empty prison slot.
     * test function for addTileToPrisonBus
     */
    @Test
    fun `add valid tile to prison bus with empty slot`() {
        val game = rootService.currentGame
        checkNotNull(game)

        //check that all members of game.prisonBuses[0].tiles are equal to null.
        //Since this is the initial state of buses on the mid.
        assertTrue(game.prisonBuses[0].tiles.all { it == null }
            , "All slots in the prison bus should initially be empty")

        val tile = game.drawStack.pop()
        rootService.playerActionService.addTileToPrisonBus(tile,game.prisonBuses[0])

        //check at least one member of the Array<Tile?> of game.prisonBuses[0].tiles are not equal to null
        assertTrue(game.prisonBuses[0].tiles.any { it != null }
            , "tile is added to the prison bus.")

    }

    /**
     * tests if player can not add a Guard Tile.
     * test function for addTileToPrisonBus
     */
    @Test
    fun `attempt to add GuardTile to prison bus`() {
        val game = rootService.currentGame
        checkNotNull(game)

        val tile = GuardTile(-1)

        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.addTileToPrisonBus(tile,game.prisonBuses[0])
        }
    }

    /**
     * tests if player can not a tile to a bus that is full.
     * test function for addTileToPrisonBus
     */
    @Test
    fun `add tile to a full prison bus`() {
        val game = rootService.currentGame
        checkNotNull(game)

        //making the bus slots full.
        val tile1 = game.drawStack.pop()
        rootService.playerActionService.addTileToPrisonBus(tile1,game.prisonBuses[0])
        val tile2 = game.drawStack.pop()
        rootService.playerActionService.addTileToPrisonBus(tile2,game.prisonBuses[0])
        val tile3 = game.drawStack.pop()
        rootService.playerActionService.addTileToPrisonBus(tile3,game.prisonBuses[0])

        val tile4 = game.drawStack.pop()
        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.addTileToPrisonBus(tile4,game.prisonBuses[0])
        }
    }

    /**
     * add a tile to a blocked slot.
     * test function for addTileToPrisonBus
     */
    @Test
    fun `add tile to blocked slot of the bus`() {
        val game = rootService.currentGame
        checkNotNull(game)

        val tile1 = game.drawStack.pop()
        //Bus 3 is now full since it has 2 blocked slots.
        rootService.playerActionService.addTileToPrisonBus(tile1,game.prisonBuses[2])

        val tile2 = game.drawStack.pop()
        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.addTileToPrisonBus(tile2,game.prisonBuses[2])
        }

    }

    /**
     * test if player can not a bus when he has already a one.
     * test function for addTileToPrisonBus
     */
    @Test
    fun `add tile when player has already taken a bus`() {
        val game = rootService.currentGame
        checkNotNull(game)

        //setting up the situation where player already taken a bus.
        val tile = game.drawStack.pop()
        rootService.playerActionService.addTileToPrisonBus(tile,game.prisonBuses[0], changePlayer = false)
        rootService.playerActionService.takePrisonBus(game.prisonBuses[0])


        val tile1 = game.drawStack.pop()
        assertFailsWith<IllegalArgumentException> {
            rootService.playerActionService.addTileToPrisonBus(tile1,game.prisonBuses[1], changePlayer = false)
        }

    }

    @Test
    fun `draw card test`() {
        val game = rootService.currentGame!!

        val topCard = game.drawStack[0]
        var actualTopCard = rootService.playerActionService.drawCard()
        assertEquals(topCard, actualTopCard)

        game.drawStack = Stack()
        val finalStackTopCard = game.finalStack[0]
        actualTopCard = rootService.playerActionService.drawCard()
        assertEquals(finalStackTopCard, actualTopCard)
    }

    /**
     * Tests the functionality of freeing a prisoner.
     * It verifies that when a player has sufficient coins and a prisoner in isolation,
     * calling the freePrisoner() function reduces the player's coins by 2 and removes the prisoner from isolation.
     */
    @Test
    fun testFreePrisoner() {
        // Set up the game state
        val game = rootService.currentGame!!
        val currentPlayer = game.players[game.currentPlayer]
        currentPlayer.coins = 5
        currentPlayer.isolation.push(PrisonerTile(1, PrisonerTrait.MALE, PrisonerType.RED))

        // Call the function under test
        rootService.playerActionService.freePrisoner()

        // Verify the expected behavior
        assertEquals(3, currentPlayer.coins)
        assertTrue(currentPlayer.isolation.isEmpty())
    }

    @Test
    fun movePrisonerToPrisonTest(){
        val players = mutableListOf(
            Pair("P1", PlayerType.PLAYER),
            Pair("P2", PlayerType.PLAYER)
        )
        rootService.gameService.startNewGame(players)
        val message = assertThrows<IllegalStateException> { rootService.playerActionService.movePrisonerToPrisonYard(10 ,10) }
        assertEquals( "Bring more money and come back!" , message.message)

        rootService.currentGame!!.players[rootService.currentGame!!.currentPlayer].coins = 10
        val message1 = assertThrows<IllegalStateException> { rootService.playerActionService.movePrisonerToPrisonYard(10 ,10) }
        assertEquals( "Empty Isolation." , message1.message)

        rootService.currentGame!!.players[rootService.currentGame!!.currentPlayer].isolation.add(PrisonerTile(0,PrisonerTrait.RICH, PrisonerType.PURPLE))
        val result = rootService.playerActionService.movePrisonerToPrisonYard(10 ,10)
        assertEquals(9, rootService.currentGame!!.players[rootService.currentGame!!.currentPlayer].coins)
        val pair1 = Pair(false, null)
        assertEquals(pair1 , result)



    }


}
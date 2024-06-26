package service

import entity.PrisonBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import java.util.Stack
import kotlin.random.Random

/**
 * Service layer class that provides basic functions related to some board elements, like creating
 * new draw and final stacks from the cards in Aquaghetto, creating prison buses, creating all tiles, checking
 * if for a new baby
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class BoardService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Creates two stacks of cards the first is the normal draw stack and the second one
     * is the final stack. Depending on the amount of players the normal stack contains different
     * This function takes these cards from the List allTiles in Aquaghetto, if there are nor
     * cards in this list, an exception is thrown
     * amount of cards.
     * 5 -> all 8 types
     * 4 -> 7 types
     * 3 -> 6 types
     * 2 -> 5 types
     *
     * @param playerCount the amount of players in the game
     * @return a Pair of the normal draw stack and the final stack
     * @throws IllegalStateException if there is no running game
     * @throws IllegalStateException if not all cards are on the list allCards
     */
    fun createStacks(playerCount: Int): Pair<Stack<Tile>,Stack<Tile>> {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        check(game.allTiles.size == 114) {"Not all cards are on the allTiles stack. Call createAllTiles first."}
        check(playerCount in 2..5) { "Not a valid amount of players."}
        val tilesInGame = mutableListOf<Tile>()

        val typesNotAdd = mutableListOf<PrisonerType>()

        when (playerCount) {
            4 -> {
                typesNotAdd.add(PrisonerType.CYAN)
            }
            3 -> {
                typesNotAdd.add(PrisonerType.CYAN)
                typesNotAdd.add(PrisonerType.BROWN)
            }
            2 -> {
                typesNotAdd.add(PrisonerType.CYAN)
                typesNotAdd.add(PrisonerType.BROWN)
                typesNotAdd.add(PrisonerType.PURPLE)
            }
        }

        val toRemove = mutableListOf<Tile>()
        for (tile in game.allTiles) {
            if (tile is PrisonerTile
                        && (tile.prisonerTrait == PrisonerTrait.BABY || typesNotAdd.contains(tile.prisonerType))) {
                    continue
            }
            tilesInGame.add(tile)
            toRemove.remove(tile)
        }
        //game.allTiles.removeAll(toRemove)

        //tilesInGame.shuffle(Random(2)) //TODO remove static seed, only for testing
        tilesInGame.shuffle()

        val finalStack = Stack<Tile>()
        finalStack.addAll(tilesInGame.reversed().subList(0 , 15))

        val normalStack = Stack<Tile>()
        normalStack.addAll(tilesInGame.subList(15 , tilesInGame.size))

        return Pair(normalStack, finalStack)
    }

    /**
     * Returns a pair containing the amount of coin tiles in the game and a map containing
     * the amount of remaining cardTypes in the game
     * This information is known to all players (by checking what cards were removed from the drawStack)
     * The information about the card type is not carried on
     *
     * @return a Pair containing the amount of coins and a map containing the amount of prisonerTypes
     */
    fun getCardsStillInGame(): Pair<Int, MutableMap<PrisonerType, Int>> {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        val allCardsLeft = mutableListOf<Tile>()
        allCardsLeft.addAll(game.drawStack)
        allCardsLeft.addAll(game.finalStack)

        val map = mutableMapOf<PrisonerType,Int>()
        var coins = 0

        for (card in allCardsLeft) {
            if (card is CoinTile) coins++
            if (card !is PrisonerTile) continue
            val current = map[card.prisonerType] ?: 0
            map[card.prisonerType] = current + 1
        }

        return Pair(coins, map)
    }

    /**
     * Creates a list of prisonBuses depending on the amount of players
     * 5, 4 or 3 players -> same amount of buses no slots blocked
     * 2 -> 3 buses, one slot blocked, two slots blocked, no slot blocked
     *
     * @param playerCount the amount of players in the game
     * @return a list of prisonBuses depending on the amount of players
     */
    fun createPrisonBuses(playerCount: Int): MutableList<PrisonBus> {
        val buses = mutableListOf<PrisonBus>()

        if (playerCount >= 3) {
            for (i in 0 until playerCount) {
                buses.add(PrisonBus().apply { index = i })
            }
        } else {
            buses.add(PrisonBus().apply { index = 2 })
            buses.add(PrisonBus().apply { blockedSlots[2] = true; index = 1 })
            buses.add(PrisonBus().apply { blockedSlots[1] = true; blockedSlots[2] = true; index = 0})
        }

        return buses
    }

    /**
     * Initializes allTiles with all cards existing in the game
     *
     * @throws IllegalStateException if there is no running game
     */
    fun createAllTiles() {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        val allTiles = mutableListOf<Tile>()

        /*Add coin tiles to list*/
        for (i in 1..10) {
            allTiles.add(CoinTile(i))
        }

        /*
         * Order of colors in Prisoner Type is equal to the Order of animals
         * red -> dolphins
         * blue -> orcas
         * green -> sea lison
         * yellow -> sea turtle
         * orange -> hippopotamus
         * purple -> crocodile
         * brown -> penguin
         * cyan -> polar bear
         */

        var count = 11
        /*dolphins, orcas, sea lison*/
        val traitsTypeA = mutableListOf(PrisonerTrait.FEMALE, PrisonerTrait.FEMALE , PrisonerTrait.MALE,
            PrisonerTrait.MALE, PrisonerTrait.NONE, PrisonerTrait.NONE, PrisonerTrait.NONE, PrisonerTrait.NONE,
            PrisonerTrait.NONE, PrisonerTrait.OLD, PrisonerTrait.OLD, PrisonerTrait.BABY, PrisonerTrait.BABY)

        /*sea turtle, hippopotamus, crocodile, penguin, polar bear*/
        val traitsTypeB = mutableListOf(PrisonerTrait.FEMALE, PrisonerTrait.FEMALE , PrisonerTrait.MALE,
            PrisonerTrait.MALE, PrisonerTrait.NONE, PrisonerTrait.RICH, PrisonerTrait.RICH, PrisonerTrait.RICH,
            PrisonerTrait.RICH, PrisonerTrait.RICH, PrisonerTrait.RICH, PrisonerTrait.BABY, PrisonerTrait.BABY)

        for (type in PrisonerType.values()) {
            if (type == PrisonerType.RED || type == PrisonerType.BLUE || type == PrisonerType.GREEN) {
                for (trait in traitsTypeA) {
                    allTiles.add(PrisonerTile(count, trait, type))
                    count++
                }
            } else {
                for (trait in traitsTypeB) {
                    allTiles.add(PrisonerTile(count, trait, type))
                    count++
                }
            }
        }

        game.allTiles = allTiles
    }

    /**
     * Function to get a baby tile of the specified type
     *
     * @param prisonerType the type of the prisoner to get a baby from
     * @return the prison tile
     * @throws IllegalStateException if no baby card is still available
     * @throws IllegalStateException if there is no running game
     */
    fun getBabyTile(prisonerType: PrisonerType): PrisonerTile {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        var tileFound: PrisonerTile? = null
        for (tile in game.allTiles) {
            if (tile is PrisonerTile && tile.prisonerTrait == PrisonerTrait.BABY && tile.prisonerType == prisonerType) {
                tileFound = tile
                break
            }
        }

        checkNotNull(tileFound) {"Found no baby card"}
        game.allTiles.remove(tileFound)
        return tileFound
    }

}
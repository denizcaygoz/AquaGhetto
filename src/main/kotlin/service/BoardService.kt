package service

import entity.PrisonBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import view.Refreshable
import java.util.Stack

class BoardService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Creates two stacks of cards the first is the normal draw stack and the second one
     * is the final stack. Depending on the amount of players the normal stack contains different
     * amount of cards.
     * 5 -> all 8 types
     * 4 -> 7 types
     * 3 -> 6 types
     * 2 -> 5 types
     *
     * @param playerCount the amount of players in the game
     * @return a Pair of the normal draw stack and the final stack
     * @throws IllegalStateException if there is no running game
     */
    fun createStacks(playerCount: Int): Pair<Stack<Tile>,Stack<Tile>> {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        val tilesInGame = mutableListOf<Tile>()

        val typesNotAdd = mutableListOf<PrisonerType>()

        when (playerCount) {
            2 -> {
                typesNotAdd.add(PrisonerType.CYAN)
            }
            3 -> {
                typesNotAdd.add(PrisonerType.CYAN)
                typesNotAdd.add(PrisonerType.BROWN)
            }
            4 -> {
                typesNotAdd.add(PrisonerType.CYAN)
                typesNotAdd.add(PrisonerType.BROWN)
                typesNotAdd.add(PrisonerType.PURPLE)
            }
        }

        val toRemove = mutableListOf<Tile>()
        for (tile in game.allTiles) {
            if ((tile is PrisonerTile && tile.prisonerTrait == PrisonerTrait.BABY) ||
                (tile is PrisonerTile && typesNotAdd.contains(tile.prisonerType))) {
                    continue
                }
            tilesInGame.add(tile)
            toRemove.remove(tile)
        }
        game.allTiles.removeAll(toRemove)

        tilesInGame.shuffle()

        val normalStack = Stack<Tile>()
        normalStack.addAll(tilesInGame.subList(0 , 15))

        val finalStack = Stack<Tile>()
        finalStack.addAll(tilesInGame.subList(15 , tilesInGame.size))

        return Pair(normalStack, finalStack)
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
                buses.add(PrisonBus())
            }
        } else {
            buses.add(PrisonBus())
            buses.add(PrisonBus().apply { blockedSlots[0] = true })
            buses.add(PrisonBus().apply { blockedSlots[0] = true; blockedSlots[1] = true})
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
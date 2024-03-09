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

    fun createStacks(playerCount: Int): Pair<Stack<Tile>,Stack<Tile>> {
        return Pair(Stack(),Stack())
    }

    fun createPrisonBusses(playerCount: Int): MutableList<PrisonBus> {
        return mutableListOf()
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

}
package service

import entity.Player
import entity.PrisonBus
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import view.Refreshable

class PlayerActionService(private val rootService: RootService): AbstractRefreshingService() {

    fun addTileToPrisonBus(tile: Tile, prisonBus: PrisonBus) {

    }

    fun takePrisonBus(prisonBus: PrisonBus) {

    }

    fun placePrisoner(tile: PrisonerTile, x: Int, y: Int): Tile? {
        return null
    }

    fun movePrisonerToPrisonYard(x: Int, y: Int) {

    }

    /*new prisoner -> sourceX = sourceY = -101*/
    /*isolation prisoner -> sourceX = sourceY = -102*/
    fun moveEmployee(sourceX: Int, sourceY: Int , destinationX: Int, destinationY: Int) {

    }

    fun buyPrisonerFromOtherIsolation(player: Player, x: Int, y: Int) {

    }

    fun freePrisoner() {

    }

    fun expandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int) {

    }

    fun checkBabyPrisoner(): PrisonerType? {
        val game = rootService.currentGame
        checkNotNull(game) { "No game started yet." }

        /*get breedable prisoners*/
        val foundBreedableMale = mutableMapOf<PrisonerType, PrisonerTile>()
        val foundBreedableFemale = mutableMapOf<PrisonerType, PrisonerTile>()
        val player = game.players[game.currentPlayer]
        val board = player.board
        for (entry1 in board.getPrisonYardIterator()) {
            val secondMap = entry1.value
            for (entry2 in secondMap) {
                val tile = entry2.value
                if (tile !is PrisonerTile) continue
                val trait = tile.prisonerTrait
                val type = tile.prisonerType
                if (trait == PrisonerTrait.MALE || tile.breedable) foundBreedableMale[type] = tile
                if (trait == PrisonerTrait.FEMALE || tile.breedable) foundBreedableFemale[type] = tile
            }
        }

        /*check for breedable prisoners*/
        for (type in PrisonerType.values()) {
            val male: PrisonerTile? = foundBreedableMale[type]
            val female: PrisonerTile? = foundBreedableMale[type]
            if (male != null && female != null) {
                male.breedable = false
                female.breedable = false
                return type
            }
        }

        /*no breedable prisoner was found*/
        return null
    }

}
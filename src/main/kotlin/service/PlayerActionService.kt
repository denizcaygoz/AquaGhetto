package service

import entity.Player
import entity.PrisonBus
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
        return null
    }

}
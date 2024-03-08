package service

import entity.tileTypes.PrisonerTile
import view.Refreshable

class ValidationService(private val rootService: RootService): Refreshable {

    fun validateTilePlacement(tile: PrisonerTile, x: Int, y: Int): Boolean {
        return false
    }

    fun validateExpandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int): Boolean {
        return false
    }

}
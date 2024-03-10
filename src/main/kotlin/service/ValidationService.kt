package service

import entity.tileTypes.PrisonerTile

/**
 * Service layer class that provides basic functions to validate if a player can make this action
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class ValidationService(private val rootService: RootService): AbstractRefreshingService() {

    fun validateTilePlacement(tile: PrisonerTile, x: Int, y: Int): Boolean {
        return false
    }

    fun validateExpandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int): Boolean {
        return false
    }

}
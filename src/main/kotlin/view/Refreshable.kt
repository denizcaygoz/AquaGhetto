package view

import entity.Player
import entity.PrisonBus
import entity.tileTypes.PrisonerTile
import service.networkService.ConnectionState

/**
 * This interface provides a mechanism for the service layer classes to communicate
 * (usually to the view classes) that certain changes have been made to the entity
 * layer, so that the user interface can be updated accordingly.
 *
 * Default (empty) implementations are provided for all methods, so that implementing
 * UI classes only need to react to events relevant to them.
 *
 * @see AbstractRefreshingService
 *
 */
interface Refreshable {

    fun refreshAfterStartGame() {}

    fun refreshAfterNextTurn(player: Player) {}

    fun refreshAfterEndGame() {}

    fun refreshPrisonBus(prisonBus: PrisonBus) {}

    fun refreshTileStack(finalStack: Boolean) {}

    fun refreshScoreStats() {}

    fun refreshPrison(tile: PrisonerTile?, x: Int, y: Int) {}

    fun refreshAfterSelectGameMode(multiplayer: Boolean) {}

    fun refreshAfterPause() {}

    fun refreshIsolation(player: Player) {}

    fun refreshEmployee(player: Player) {}

    fun refreshConnectionState(state: ConnectionState) {}



}
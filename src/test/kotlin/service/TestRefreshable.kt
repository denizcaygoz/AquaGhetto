package service

import entity.Player
import entity.PrisonBus
import entity.tileTypes.Tile
import view.Refreshable

/**
 * [Refreshable] implementation that refreshes nothing, but remembers
 * if a refresh method has been called (since last [reset])
 */
class TestRefreshable: Refreshable {
    var refreshAfterStartGameCalled: Boolean = false
        private set
    var refreshAfterNextTurnCalled: Boolean = false
        private set
    var refreshAfterEndGameCalled: Boolean = false
        private set
    var refreshPrisonBusCalled: Boolean = false
        private set
    var refreshTileStackCalled: Boolean = false
        private set
    var refreshScoreStatsCalled: Boolean = false
        private set
    var refreshPrisonCalled: Boolean = false
        private set
    var refreshAfterSelectGameModeCalled: Boolean = false
        private set
    var refreshAfterPauseCalled: Boolean = false
        private set
    var refreshIsolationCalled: Boolean = false
        private set
    var refreshEmployeeCalled: Boolean = false
        private set

    /**
     * Resets all refresh flags to their initial state.
     */
    fun reset() {
        refreshAfterStartGameCalled = false
        refreshAfterNextTurnCalled = false
        refreshAfterEndGameCalled = false
        refreshPrisonBusCalled = false
        refreshTileStackCalled = false
        refreshScoreStatsCalled = false
        refreshPrisonCalled = false
        refreshAfterSelectGameModeCalled = false
        refreshAfterPauseCalled = false
        refreshIsolationCalled = false
        refreshEmployeeCalled = false
    }

    /**
     * [refreshAfterStartGame] sets [refreshAfterStartGameCalled] to true.
     */
     override fun refreshAfterStartGame() {
        refreshAfterStartGameCalled = true
     }

    /**
     * [refreshAfterNextTurn] sets [refreshAfterNextTurnCalled] to true.
     */
    override fun refreshAfterNextTurn(player: Player) {
        refreshAfterNextTurnCalled = true
    }

    /**
     * [refreshAfterEndGame] sets [refreshAfterEndGameCalled] to true.
     */
    override fun refreshAfterEndGame() {
        refreshAfterEndGameCalled = true
    }

    /**
     * [refreshPrisonBus] sets [refreshPrisonBusCalled] to true.
     */
    override fun refreshPrisonBus(prisonBus: PrisonBus?) {
        refreshPrisonBusCalled = true
    }

    /**
     * [refreshTileStack] sets [refreshTileStackCalled] to true.
     */
    override fun refreshTileStack(finalStack: Boolean) {
        refreshTileStackCalled = true
    }

    /**
     * [refreshScoreStats] sets [refreshScoreStatsCalled] to true.
     */
    override fun refreshScoreStats() {
        refreshScoreStatsCalled = true
    }

    /**
     * [refreshPrison] sets [refreshPrisonCalled] to true.
     */
    override fun refreshPrison(tile: Tile?, x: Int, y: Int) {
        refreshPrisonCalled = true
    }

    /**
     * [refreshAfterSelectGameMode] sets [refreshAfterSelectGameModeCalled] to true.
     */
    override fun refreshAfterSelectGameMode(multiplayer: Boolean) {
        refreshAfterSelectGameModeCalled = true
    }

    /**
     * [refreshAfterPause] sets [refreshAfterPauseCalled] to true.
     */
    override fun refreshAfterPause() {
        refreshAfterPauseCalled = true
    }

    /**
     * [refreshIsolation] sets [refreshIsolationCalled] to true.
     */
    override fun refreshIsolation(player: Player) {
        refreshIsolationCalled = true
    }

    /**
     * [refreshGuards] sets [refreshEmployeeCalled] to true.
     */
    override fun refreshGuards(player: Player, sourceCoords: Pair<Int, Int>?, destCoords: Pair<Int, Int>?) {
        refreshEmployeeCalled = true
    }
}
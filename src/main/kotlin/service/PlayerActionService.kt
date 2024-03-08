package service

import entity.AquaGhetto
import entity.Player
import entity.PrisonBus
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import view.Refreshable

class PlayerActionService(private val rootService: RootService): Refreshable {

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

    /**
     * [freePrisoner] discards the tile on top of the isolation stack.
     *
     * @throws IllegalStateException when there is no game running
     * @throws IllegalArgumentException when the player has not enough money
     * @throws IllegalArgumentException when there is no prisoner on his isolation stack
     *
     **/
    fun freePrisoner() {
        val game: AquaGhetto? = rootService.currentGame

        checkNotNull(game) { "There is no game running" }

        val currentPlayer: Player = game.players[game.currentPlayer]
        require(currentPlayer.money >= 2) { "The current player has not enough money" }
        require(!currentPlayer.isolation.empty()) { "There is not prisoner to be freed" }

        currentPlayer.isolation.pop()
        currentPlayer.money -= 2

        rootService.evaluationService.evaluatePlayer(currentPlayer)

        //rootService.gameService.determineNextPlayer()

        /**
         * onAllRefreshables { refreshIsolation() }
         * */
    }

    fun expandPrisonGrid(isBigExtension: Boolean, x: Int, y: Int , rotation: Int) {

    }

    fun checkBabyPrisoner(): PrisonerType? {
        return null
    }

}
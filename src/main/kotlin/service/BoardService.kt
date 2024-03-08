package service

import entity.PrisonBus
import entity.tileTypes.Tile
import view.Refreshable
import java.util.Stack

class BoardService(private val rootService: RootService): AbstractRefreshingService() {

    fun createStacks(playerCount: Int): Pair<Stack<Tile>,Stack<Tile>> {
        return Pair(Stack(),Stack())
    }

    fun createPrisonBusses(playerCount: Int): List<PrisonBus> {
        return mutableListOf()
    }

    fun createAllTiles() {

    }

}
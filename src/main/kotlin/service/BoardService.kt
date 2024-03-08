package service

import entity.PrisonBus
import entity.tileTypes.Tile
import java.util.Stack

class BoardService(private val rootService: RootService) {

    fun createStacks(playerCount: Int): Pair<Stack<Tile>,Stack<Tile>> {
        return Pair(Stack(),Stack())
    }

    fun createPrisonBusses(playerCount: Int): List<PrisonBus> {
        return mutableListOf()
    }

    fun createAllTiles() {

    }

}
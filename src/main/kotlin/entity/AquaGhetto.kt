package entity

import entity.tileTypes.Tile
import tools.aqua.bgw.util.Stack

class AquaGhetto {

    val drawStack: Stack<Tile> = Stack()
    val finalStack: Stack<Tile> = Stack()
    val players: MutableList<Player> = mutableListOf()
    val currentPlayer: Int = 0
    val prisonBusses: MutableList<PrisonBus> = mutableListOf()


}
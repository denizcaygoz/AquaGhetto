package entity

import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import tools.aqua.bgw.util.Stack

class Player(val name: String , val board: Board) {

    val isolation: Stack<PrisonerTile> = Stack()
    val takenBus: PrisonBus? = null
    val employees: MutableList<GuardTile> = mutableListOf()
    val money: Int = 0
    val hasJanitor = false
    val secretaryCount = 0
    val layerCount = 0
    val remainingBigExtensions: Int = 2
    val remainingSmallExtensions: Int = 2

}
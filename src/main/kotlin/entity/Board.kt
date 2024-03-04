package entity

import entity.tileTypes.Tile

class Board {

    private val prisonYard: MutableMap<Int, MutableMap<Int, Tile>> = mutableMapOf()
    private val prisonGrid: MutableMap<Int, MutableMap<Int, Boolean>> = mutableMapOf()
    val guardPosition: MutableList<Pair<Int, Int>> = mutableListOf()

    fun getPrisonYard(x: Int, y: Int): Tile? {
        val yMap = prisonYard[x] ?: return null
        return yMap[y]
    }

    fun getPrisonGrid(x: Int, y: Int): Boolean {
        val yMap = prisonGrid[x] ?: return false
        return yMap[y] ?: return false
    }

}
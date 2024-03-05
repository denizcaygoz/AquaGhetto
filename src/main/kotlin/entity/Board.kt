package entity

import entity.tileTypes.Tile
import java.io.Serializable

/**
 * A data class to represent a player's personal game board
 *
 * The class contains two two-dimensional maps for saving the tiles placed and the prison floor plan.
 * There is also a list containing the positions of the guard tiles (the guard tiles are still saved in the map).
 *
 * @property prisonYard a two-dimensional map containing the tiles placed by the player. The first key is
 * the x-coordinate of the grid (horizontal), the second key is the y-coordinate of the grid (vertical).
 *
 * @property prisonGrid a two-dimensional map containing boolean values. The first key is
 * the x-coordinate of the grid (horizontal), the second key is the y-coordinate of the grid (vertical).
 * The boolean value defines if there is a prison area. This value is still true if there
 * is already a tile on this location
 *
 * @property guardPosition a list of [Pair] containing the locations of the guards placed on the yard.
 */
class Board: Serializable {

    private val prisonYard: MutableMap<Int, MutableMap<Int, Tile>> = mutableMapOf()
    private val prisonGrid: MutableMap<Int, MutableMap<Int, Boolean>> = mutableMapOf()
    val guardPosition: MutableList<Pair<Int, Int>> = mutableListOf()

    /**
     * A function to get the tile at the specified location
     * returns null if there is no tile at the specified location
     *
     * @param x the x-coordinate of the grid
     * @param y the y-coordinate of the grid
     * @return the tile at the location or null if there is no tile
     */
    fun getPrisonYard(x: Int, y: Int): Tile? {
        val yMap = prisonYard[x] ?: return null
        return yMap[y]
    }

    /**
     * A function to set/remove the tile at the specified location
     *
     * @param x the x-coordinate of the grid
     * @param y the y-coordinate of the grid
     * @param tile the to set at the specified location, can be null to remove the tile at the specified location
     */
    fun setPrisonYard(x: Int, y: Int , tile: Tile?) {
        var yMap = prisonYard[x]
        if (yMap == null) {
            yMap = mutableMapOf()
            prisonYard[x] = yMap
        }
        if (tile != null) {
            yMap[y] = tile
        } else {
            yMap.remove(y)
        }
    }

    /**
     * A function to check if there is a prison area at the specified location
     *
     * @param x the x-coordinate of the grid
     * @param y the y-coordinate of the grid
     * @return true if there is a prison are or false if not
     */
    fun getPrisonGrid(x: Int, y: Int): Boolean {
        val yMap = prisonGrid[x] ?: return false
        return yMap[y] ?: return false
    }

    /**
     * A function to set if there is a prison flor at the specified location
     *
     * @param x the x-coordinate of the grid
     * @param y the y-coordinate of the grid
     * @param floor if there should be a floor at the specified location or not
     */
    fun setPrisonGrid(x: Int, y: Int , floor: Boolean) {
        var yMap = prisonGrid[x]
        if (yMap == null) {
            yMap = mutableMapOf()
            prisonGrid[x] = yMap
        }
        yMap[y] = floor
    }

}
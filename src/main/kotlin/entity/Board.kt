package entity

import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
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
 * This map will only hold [PrisonerTile] and [GuardTile]. This is ensured by the [setPrisonYard] function.
 *
 * @property prisonGrid a two-dimensional map containing boolean values. The first key is
 * the x-coordinate of the grid (horizontal), the second key is the y-coordinate of the grid (vertical).
 * The boolean value defines if there is a prison area. This value is still true if there
 * is already a tile on this location
 *
 * @property guardPosition a list of [Pair] containing the locations of the guards placed on the yard.
 */
class Board: Serializable, Cloneable {
    companion object {
        private const val serialVersionUID: Long = -5505146300869386926L
    }

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
     * @throws IllegalArgumentException if the tile is not a [PrisonerTile] or a [GuardTile] or null
     */
    fun setPrisonYard(x: Int, y: Int , tile: Tile?) {
        require(tile == null || tile is PrisonerTile || tile is GuardTile)
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
     * Function to get an iterator of the prison yard map
     *
     * @return a [MutableIterator] over the prison yard map
     */
    fun getPrisonYardIterator(): MutableIterator<MutableMap.MutableEntry<Int, MutableMap<Int, Tile>>> {
        return prisonYard.iterator()
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

    /**
     * Function to get an iterator of the prison grid map
     *
     * @return a [MutableIterator] over the prison grid map
     */
    fun getPrisonGridIterator(): MutableIterator<MutableMap.MutableEntry<Int, MutableMap<Int, Boolean>>> {
        return prisonGrid.iterator()
    }

    public override fun clone(): Board {
        return Board().apply {
            guardPosition.addAll(this@Board.guardPosition)
            for (xIterator in this@Board.getPrisonGridIterator()) {
                for (yIterator in xIterator.value) {
                    val floor = this@Board.getPrisonGrid(xIterator.key, yIterator.key)
                    this.setPrisonGrid(xIterator.key, yIterator.key, floor)
                }
            }

            for (xIterator in this@Board.getPrisonYardIterator()) {
                for (yIterator in xIterator.value) {
                    val tile = this@Board.getPrisonYard(xIterator.key, yIterator.key)?.let {
                        if (it is PrisonerTile) it.clone() else it
                    }
                    if (tile is PrisonerTile) {
                        this.setPrisonYard(xIterator.key, yIterator.key, tile.clone())
                    } else {
                        this.setPrisonYard(xIterator.key, yIterator.key, tile)
                    }

                }
            }
        }
    }
}
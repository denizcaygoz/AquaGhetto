package entity.tileTypes

import java.io.Serializable

/**
 * Data class to represent a guard tile
 *
 * This would be a manager in the game Aquaretto
 *
 * @see [Tile]
 */
class GuardTile : Tile(), Serializable {
    companion object {
        private const val serialVersionUID: Long = -8828161763151770395L
    }


}
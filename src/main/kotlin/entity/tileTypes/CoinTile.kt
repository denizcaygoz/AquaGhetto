package entity.tileTypes

import java.io.Serializable

/**
 * Data class to represent a coin tile
 * @see [Tile]
 */
class CoinTile: Tile(), Serializable {
    companion object {
        private const val serialVersionUID = 1780905378431693221L
    }

}

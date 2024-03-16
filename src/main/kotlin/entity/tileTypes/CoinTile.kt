package entity.tileTypes

import java.io.Serializable

/**
 * Data class to represent a coin tile
 * @see [Tile]
 *
 * @param id the unique identifier of this card
 */
data class CoinTile(override val  id:Int): Tile(), Serializable {
    companion object {
        private const val serialVersionUID = 1780905378431693221L
    }

}


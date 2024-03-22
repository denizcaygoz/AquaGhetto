package entity.tileTypes

import java.io.Serializable

/**
 * Data class to represent a tile
 * During the game, tiles are placed on the player's own [Board], in isolation or temporarily in the [PrisonBus]
 *
 * @property id the unique identifier of this card
 */
abstract class Tile: Serializable {


    abstract val id: Int
    companion object {
        private const val serialVersionUID: Long = -1344851655740130092L
    }

}
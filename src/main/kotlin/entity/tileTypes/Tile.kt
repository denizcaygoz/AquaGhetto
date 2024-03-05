package entity.tileTypes

import java.io.Serializable

/**
 * Data class to represent a tile
 * During the game, tiles are placed on the player's own [Board], in isolation or temporarily in the [PrisonBus]
 */
abstract class Tile: Serializable
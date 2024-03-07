package entity

import entity.tileTypes.Tile
import java.io.Serializable

/**
 * Data class to represent a prisoner bus
 *
 * This would be a truck in the game Aquaretto
 *
 * @property tiles an array with 3 positions of [Tile] in the prison bus, elements can be null if there is no card
 * @property blockedSlots an BooleanArray with 3 positions, defines if a slot in a prison bus is blocked
 */
class PrisonBus: Serializable {
    companion object {
        private const val serialVersionUID: Long = -3274357975281180850L
    }

    val tiles: Array<Tile?> = Array(3) {null}
    val blockedSlots: BooleanArray = BooleanArray(3) {false}

}
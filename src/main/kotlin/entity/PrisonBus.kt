package entity

import entity.tileTypes.Tile
import java.io.Serial
import java.io.Serializable

/**
 * Data class to represent a prisoner bus
 *
 * This would be a truck in the game Aquaretto
 *
 * @property tiles a list of [Tile] in the prison bus
 * @property blockedSlots a list of [Boolean], defines if a slot in a prison bus is blocked
 * if this list does not contain a value, no slot is blocked
 */
class PrisonBus: Serializable {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -3274357975281180850L
    }

    val tiles: MutableList<Tile> = mutableListOf()
    val blockedSlots: MutableList<Boolean> = mutableListOf()

}
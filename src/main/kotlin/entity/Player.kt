package entity

import entity.tileTypes.PrisonerTile
import java.io.Serializable
import java.util.Stack

/**
 * Data class to represent a player in the game Aquaghetto.
 *
 * Besides having a [name], the player consist of a [Board], a [Stack] containing [PrisonerTile],
 * a [PrisonBus], an [Int] storing the player's money, a [Boolean] storing if a player has a janitor,
 * an [Int] storing the amount of secretaries the player owns, an [Int] storing the amount of layer the
 * player owns, an [Int] storing the amount of big extension the player can use, an [Int] storing the amount of
 * small extension the player can use, an [Int] storing the amount of different prisonerTypes a player can have
 *
 * @param name a string to store the name of the player
 * @property board the player's own [board].
 * @property isolation a [Stack] containing [PrisonerTile], this would be the depot in Aquaretto
 * @property takenBus the [PrisonBus] a player is currently holding, can be null if the player does not own a bus
 * if the player owns a bus this indicates that has finished his actions for this round
 * @property money the amount of money the player owns
 * @property hasJanitor if the player owns a janitor, this would be the manager in Aquaretto
 * @property secretaryCount the amount of secretaries the players owns, a value between 0 and 2 (included), this
 * would be the cashier in Aquaretto
 * @property lawyerCount the amount of layers the player owns, a value between 0 and 2 (included), this
 * would be the keeper in Aquaretto
 * @property remainingBigExtensions the amount of big extensions the player could place
 * @property remainingSmallExtensions the amount of small extensions the player could place
 * @property maxPrisonerTypes the maximum amount of prisoners with different types
 * @property currentScore the current score of a player
 */

class Player(val name: String): Serializable {
    companion object {
        private const val serialVersionUID: Long = 2382752881182402781L
    }

    val board: Board = Board()
    val isolation: Stack<PrisonerTile> = Stack()
    var takenBus: PrisonBus? = null
    var money: Int = 0
    var hasJanitor = false
    var secretaryCount = 0
    var lawyerCount = 0
    var remainingBigExtensions: Int = 2
    var remainingSmallExtensions: Int = 2
    var maxPrisonerTypes = 3
    var currentScore = 0

}

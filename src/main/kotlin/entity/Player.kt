package entity

import entity.enums.PlayerType
import entity.tileTypes.PrisonerTile
import java.io.Serializable
import java.util.*

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
 * @param type the type of this player
 * @property board the player's own [board].
 * @property isolation a [Stack] containing [PrisonerTile], this would be the depot in Aquaretto
 * @property takenBus the [PrisonBus] a player is currently holding, can be null if the player does not own a bus
 * if the player owns a bus this indicates that has finished his actions for this round
 * @property coins the amount of coins the player owns
 * @property hasJanitor if the player owns a janitor, this would be the manager in Aquaretto
 * @property secretaryCount the amount of secretaries the players owns, a value between 0 and 2 (included), this
 * would be the cashier in Aquaretto
 * @property lawyerCount the amount of layers the player owns, a value between 0 and 2 (included), this
 * would be the keeper in Aquaretto
 * @property remainingBigExtensions the amount of big extensions the player could place
 * @property remainingSmallExtensions the amount of small extensions the player could place
 * @property maxPrisonerTypes the maximum amount of prisoners with different types
 * @property currentScore the current score of a player
 * @property delayTime amount of time the AI needs to wait before making a Turn
 */

class Player(val name: String, val type: PlayerType): Serializable, Cloneable {
    companion object {
        private const val serialVersionUID: Long = 2382752881182402781L
    }

    var board: Board = Board()
    var isolation: Stack<PrisonerTile> = Stack()
    var takenBus: PrisonBus? = null
    var coins: Int = 1
    var hasJanitor = false
    var secretaryCount = 0
    var lawyerCount = 0
    var remainingBigExtensions: Int = 2
    var remainingSmallExtensions: Int = 2
    var maxPrisonerTypes = 3
    var currentScore = 0

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): Player {
        return Player(name, type).apply {
            board = this@Player.board.clone()
            isolation = this@Player.isolation.clone() as Stack<PrisonerTile>
            takenBus = this@Player.takenBus?.clone()
            coins = this@Player.coins
            hasJanitor = this@Player.hasJanitor
            secretaryCount = this@Player.secretaryCount
            lawyerCount = this@Player.lawyerCount
            remainingSmallExtensions = this@Player.remainingSmallExtensions
            remainingBigExtensions = this@Player.remainingBigExtensions
            maxPrisonerTypes = this@Player.maxPrisonerTypes
            currentScore = this@Player.currentScore
        }
    }

}

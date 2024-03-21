package entity.aIActions

/**
 * Represents an AI action of expanding the prison.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @property isBig Indicates whether the expansion is big.
 * @property location The location where the expansion occurs, represented as a pair of integers (x, y).
 * @property rotation The rotation of the expansion.
 * @constructor Initializes an ActionExpandPrison object with the specified parameters.
 */
class ActionExpandPrison(validAction: Boolean,
                         score: Int,
                         val isBig: Boolean,
                         val location: Pair<Int,Int>,
                         val rotation: Int): AIAction(validAction, score)
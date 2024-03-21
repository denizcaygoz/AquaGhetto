package entity.aIActions

/**
 * Represents an AI action.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @constructor Initializes an AIAction object with the specified parameters.
 */
open class AIAction(val validAction: Boolean,
                    var score: Int)
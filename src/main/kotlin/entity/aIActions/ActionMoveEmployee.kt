package entity.aIActions

/**
 * Represents an AI action of moving an employee from one location to another.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @property source The source location from which the employee is moved.
 * @property destination The destination location to which the employee is moved.
 * @constructor Initializes an ActionMoveEmployee object with the specified parameters.
 */
class ActionMoveEmployee(validAction: Boolean,
                         score: Int,
                         val source: Pair<Int,Int>,
                         val destination: Pair<Int,Int>): AIAction(validAction, score)
package entity.aIActions

/**
 * Represents an AI action of taking a bus with assigned place cards.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @property bus The index of the bus to take.
 * @property placeCards The list of place cards assigned to the bus.
 * @constructor Initializes an ActionTakeBus object with the specified parameters.
 */
class ActionTakeBus(validAction: Boolean,
                    score: Int,
                    val bus: Int,
                    val placeCards: MutableList<PlaceCard>): AIAction(validAction, score)
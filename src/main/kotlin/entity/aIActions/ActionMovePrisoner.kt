package entity.aIActions

/**
 * Represents an AI action of moving a prisoner to a different location on the game board.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @property placeCard The place card indicating the new location for the prisoner.
 * @constructor Initializes an ActionMovePrisoner object with the specified parameters.
 */
class ActionMovePrisoner(validAction: Boolean,
                         score: Int,
                         val placeCard: PlaceCard): AIAction(validAction, score)
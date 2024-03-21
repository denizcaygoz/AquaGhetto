package entity.aIActions

/**
 * Represents an AI action of freeing a prisoner.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @constructor Initializes an ActionFreePrisoner object with the specified parameters.
 */
class ActionFreePrisoner(validAction: Boolean,
                         score: Int): AIAction(validAction, score)
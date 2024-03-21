package entity.aIActions

import entity.Player

/**
 * Represents an AI action of buying a prisoner.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @property buyFrom The player from whom the prisoner is bought.
 * @property placeCard The place card associated with this action.
 * @constructor Initializes an ActionBuyPrisoner object with the specified parameters.
 */
class ActionBuyPrisoner(validAction: Boolean,
                        score: Int,
                        val buyFrom: Player,
                        val placeCard: PlaceCard): AIAction(validAction, score)
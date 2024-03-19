package entity.aIActions

import entity.Player

class ActionBuyPrisoner(validAction: Boolean,
                        score: Int,
                        val buyFrom: Player,
                        val placeCard: PlaceCard): AIAction(validAction, score)
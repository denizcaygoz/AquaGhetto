package entity.aIActions

import entity.Player

class ActionBuyPrisoner(score: Int,
                        val buyFrom: Player,
                        val placeCard: PlaceCard): AIAction(score)
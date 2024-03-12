package entity.aIActions

class ActionTakeBus(score: Int,
                    val placeCardA: PlaceCard,
                    val placeCardB: PlaceCard? = null,
                    val placeCardC: PlaceCard? = null): AIAction(score)
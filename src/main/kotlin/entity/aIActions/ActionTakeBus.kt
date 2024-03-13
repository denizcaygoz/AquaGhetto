package entity.aIActions

class ActionTakeBus(validAction: Boolean,
                    score: Int,
                    val placeCardA: PlaceCard,
                    val placeCardB: PlaceCard? = null,
                    val placeCardC: PlaceCard? = null): AIAction(validAction, score)
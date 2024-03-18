package entity.aIActions

class ActionTakeBus(validAction: Boolean,
                    score: Int,
                    val bus: Int,
                    val placeCards: MutableList<PlaceCard>): AIAction(validAction, score)
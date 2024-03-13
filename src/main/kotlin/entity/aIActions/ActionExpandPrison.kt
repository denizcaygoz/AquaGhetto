package entity.aIActions

class ActionExpandPrison(validAction: Boolean,
                         score: Int,
                         val isBig: Boolean,
                         val location: Pair<Int,Int>,
                         val rotation: Int): AIAction(validAction, score)
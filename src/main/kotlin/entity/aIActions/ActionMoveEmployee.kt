package entity.aIActions

class ActionMoveEmployee(validAction: Boolean,
                         score: Int,
                         val source: Pair<Int,Int>,
                         val destination: Pair<Int,Int>): AIAction(validAction, score)
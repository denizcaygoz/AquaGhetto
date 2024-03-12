package entity.aIActions

class ActionMoveEmployee(score: Int,
                         val source: Pair<Int,Int>,
                         val destination: Pair<Int,Int>): AIAction(score)
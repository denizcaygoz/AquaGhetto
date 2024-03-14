package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionMoveEmployee
import service.aiServices.smart.SmartAI

class EvaluateMoveEmployeeService(val smartAI: SmartAI) {

    fun getScoreMoveEmployee(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionMoveEmployee {
        return ActionMoveEmployee(false, 0, Pair(0,0), Pair(0,0))
    }

}
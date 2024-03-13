package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionExpandPrison
import service.aiServices.smart.SmartAI

class EvaluateExpandPrisonGridService(val smartAI: SmartAI) {

    fun getScoreExpandPrisonGrid(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionExpandPrison {
        return ActionExpandPrison(false, 0, Pair(0,0) , 0)
    }

}
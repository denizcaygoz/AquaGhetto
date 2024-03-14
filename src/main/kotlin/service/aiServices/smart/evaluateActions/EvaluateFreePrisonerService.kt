package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionFreePrisoner
import service.aiServices.smart.SmartAI

class EvaluateFreePrisonerService(val smartAI: SmartAI) {

    fun freePrisoner(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionFreePrisoner {
        return ActionFreePrisoner(false, 0)
    }

}
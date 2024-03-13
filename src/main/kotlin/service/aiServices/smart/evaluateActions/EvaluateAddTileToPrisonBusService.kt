package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionAddTileToBus
import service.aiServices.smart.SmartAI

class EvaluateAddTileToPrisonBusService(val smartAI: SmartAI) {

    fun getScoreAddTileToPrisonBus(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionAddTileToBus {
        return ActionAddTileToBus(false, 0, 0)
    }

}
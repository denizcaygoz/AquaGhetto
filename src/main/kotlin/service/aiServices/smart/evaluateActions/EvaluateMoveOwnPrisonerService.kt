package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionMovePrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI

class EvaluateMoveOwnPrisonerService(val smartAI: SmartAI) {

    fun getScoreMoveOwnPrisoner(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionMovePrisoner {
        return ActionMovePrisoner(false, 0, PlaceCard(Pair(0,0)))
    }

}
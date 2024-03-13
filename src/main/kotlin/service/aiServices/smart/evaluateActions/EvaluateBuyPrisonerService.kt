package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionBuyPrisoner
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI

class EvaluateBuyPrisonerService(val smartAI: SmartAI) {

    fun getScoreBuyPrisoner(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionBuyPrisoner {
        return ActionBuyPrisoner(false, 0, smartAI.player, PlaceCard(Pair(0,0)))
    }

}
package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.aIActions.ActionTakeBus
import entity.aIActions.PlaceCard
import service.aiServices.smart.SmartAI

class EvaluateTakeBusService(val smartAI: SmartAI) {

    fun takeBus(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionTakeBus {
        return ActionTakeBus(false, 0, PlaceCard(Pair(0,0)))
        /*erneuter aufruf von minmax um den score der nächsten aktion zu bekommen, hier nur score relevant*/
        /*gilt für alle methoden die ab hier kommen*/
        /*am besten kann man das hier auch auslagern später in eigende Klassen*/
    }


}
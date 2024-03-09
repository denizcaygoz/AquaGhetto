package service

import entity.Player
import entity.enums.PrisonerType

class EvaluationService(private val rootService: RootService): AbstractRefreshingService() {

    fun evaluatePlayer(player: Player): Int {
        return 0
    }

    fun evaluateGame() {

    }

    fun getPrisonerTypeCount(player: Player): MutableMap<PrisonerType, Int> {
        return mutableMapOf()
    }

}
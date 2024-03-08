package service

import entity.Player
import entity.enums.PrisonerType
import view.Refreshable

class EvaluationService(private val rootService: RootService): Refreshable {

    fun evaluatePlayer(player: Player): Int {
        return 0
    }

    fun evaluateGame() {

    }

    fun getPrisonerTypeCount(player: Player): MutableMap<PrisonerType, Int> {
        return mutableMapOf()
    }

}
package service

import entity.AquaGhetto
import view.Refreshable

class RootService {

    val boardService = BoardService(this)
    val evaluationService = EvaluationService(this)
    val gameService = GameService(this)
    val gameStatesService = GameStatesService(this)
    val aiService = AIService(this)
    val playerActionService = PlayerActionService(this)
    val validationService = ValidationService(this)

    var currentGame: AquaGhetto? = null

    /**
     * Adds the provided [newRefreshable] to all services connected
     * to this root service
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        boardService.addRefreshable(newRefreshable)
        evaluationService.addRefreshable(newRefreshable)
        gameService.addRefreshable(newRefreshable)
        gameStatesService.addRefreshable(newRefreshable)
        aiService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
        validationService.addRefreshable(newRefreshable)
    }

}
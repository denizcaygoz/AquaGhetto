package service

import entity.AquaGhetto

class RootService {

    val boardService = BoardService(this)
    val evaluationService = EvaluationService(this)
    val gameService = GameService(this)
    val gameStatesService = GameStatesService(this)
    val aiService = AIService(this)
    val playerActionService = PlayerActionService(this)
    val validationService = ValidationService(this)

    var currentGame: AquaGhetto? = null

}
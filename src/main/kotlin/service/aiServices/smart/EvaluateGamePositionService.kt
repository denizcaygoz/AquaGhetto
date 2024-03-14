package service.aiServices.smart

class EvaluateGamePositionService(val smartAI: SmartAI) {

    fun evaluateCurrentPosition(): Int {
        val game = smartAI.rootService.currentGame
        checkNotNull(game) { "No running game." }
        return smartAI.rootService.evaluationService.evaluatePlayer(game.players[game.currentPlayer])
    }

}
package service.aiServices

import entity.Player
import entity.enums.PlayerType
import service.AbstractRefreshingService
import service.RootService
import service.aiServices.random.RandomAIService
import service.aiServices.smart.SmartAI

/**
 * Service layer class that provides one public method to determine the next move of an AI / a function
 * that maes this turn
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class AIService(private val rootService: RootService): AbstractRefreshingService() {

    private val randomAIService = RandomAIService(rootService)
    private val playerAIServiceMap = mutableMapOf<String, SmartAI>()

    /**
     * Executes one turn of an AI
     *
     * Depending on the type of the provided player the AI makes different actions:
     * AI -> smart move
     * RANDOM_AI -> random move
     * PLAYER, NETWORK -> throws exception
     *
     * @param player the player in whose name the action is performed, has to be the current player
     * @param delay the total delay of this action, if the computing of the turn already takes longer than delay
     * there is no additional delay. Delay is measured in milliseconds
     * @throws IllegalArgumentException if the provided player is not an AI or a RandomAI
     * @throws IllegalArgumentException if the provided player is not the current player
     */
    fun makeTurn(player: Player, delay: Int) {
        /*get the time when function is called*/
        val startTime = System.currentTimeMillis()

        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }

        require(player.type == PlayerType.AI || player.type == PlayerType.RANDOM_AI) {"player need to be an ai"}
        require(player == game.players[game.currentPlayer]) {"provided player has to be the current player"}

        if (player.type == PlayerType.RANDOM_AI) {
            randomAIService.randomAITurn(player, game)
        } else {

            /*get the instance for the player*/
            var serviceAI = playerAIServiceMap[player.name]

            /*create new instance if no exist*/
            if (serviceAI == null) {
                serviceAI = SmartAI(rootService, player)
                playerAIServiceMap[player.name] = serviceAI
            }

            /*make turn*/
            serviceAI.makeTurn(game.clone())
        }

        /*wait until delay is over*/
        val endTime = System.currentTimeMillis()
        Thread.sleep(Integer.max((delay) - (endTime - startTime).toInt() , 0).toLong())
    }


}
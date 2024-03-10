package service

import entity.Player

/**
 * Service layer class that provides one public method to determine the next move of an AI / a function
 * that maes this turn
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class AIService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Executes one turn of an AI
     *
     * Depending on the type of the provided player the AI makes different actions:
     * AI -> smart move
     * RANDOM_AI -> random move
     * PLAYER, NETWORK -> throws exception
     *
     * @param player the player in whose name the action is performed
     * @param delay the total delay of this action, if the computing of the turn already takes longer than delay
     * there is no additional delay. Delay is measured in milliseconds
     * @throws IllegalArgumentException if the provides player is not an AI or a RandomAI
     */
    fun makeTurn(player: Player, delay: Int) {

    }


}
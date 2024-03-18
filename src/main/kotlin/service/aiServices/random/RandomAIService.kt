package service.aiServices.random

import entity.AquaGhetto
import entity.Player
import service.RootService
import java.util.Random

/**
 * Service class providing a function executing a turn for a Random AI
 *
 * @param rootService instance of the [RootService] for access to other services
 * @property randomAICheckValidService an instance of [RandomAICheckValidService] for checking valid actions
 */
class RandomAIService(rootService: RootService) {

    val randomAICheckValidService = RandomAICheckValidService(rootService)

    private val ran = Random()
    private val randomAIActionService = RandomAIActionService(rootService, this)

    /**
     * Function for executing a players turn
     *
     * This function randomly executes an action for the current player
     * @param player the random AI player, has to be the current player
     * @param game the current instance of AquaGhetto
     */
    fun randomAITurn(player: Player, game: AquaGhetto) {
        val canTakeBuses = randomAICheckValidService.canTakePrisonBus(player, game)
        val canPlaceTile = randomAICheckValidService.canPlaceTileOnBus(player, game)
        val canBuyOtherPrisoners = randomAICheckValidService.canBuyOtherPrisoner(player, game)

        val validOptions = mutableListOf(
            canPlaceTile.isNotEmpty(),
            randomAICheckValidService.canMoveOwnPrisoner(player),
            randomAICheckValidService.canMoveEmployee(player),
            canBuyOtherPrisoners.isNotEmpty(),
            randomAICheckValidService.canFreeOwnPrisoner(player),
            randomAICheckValidService.canBuyExpansion(player),
            canTakeBuses.isNotEmpty()
        )

        val option = getRandomValidOption(validOptions)
        require(option != -1) {"No valid options, how did this happen?"}

        when (option) {
            0 -> { /*place tile on prison bus*/
                randomAIActionService.addTileToPrisonBus(canTakeBuses.toList())
            }
            1 -> { /*move own prisoner from isolation*/
                randomAIActionService.moveOwnPrisonerFromIsolation(player)
            }
            2 -> { /*move employee*/
                randomAIActionService.moveEmployee(player)
            }
            3 -> { /*buy prisoner from other isolation*/
                randomAIActionService.buyPrisonerFromOtherIsolation(canBuyOtherPrisoners.toList(), player)
            }
            4 -> { /*free prisoner from own isolation*/
                randomAIActionService.freePrisonerFromOwnIsolation()
            }
            5 -> { /*expand prison yard*/
                randomAIActionService.expandPrisonGrid(player)
            }
            6 -> { /*take prison bus*/
                randomAIActionService.takePrisonBus(canTakeBuses.toList() , player)
            }
        }

    }

    /**
     * Function which randomly selects the index of a boolean which is true
     *
     * @param validOptions a list of booleans
     * @return the index of a boolean which is true
     */
    fun getRandomValidOption(validOptions: MutableList<Boolean>): Int {
        var amountValid = 0
        for (option in validOptions) {
            if (option) amountValid++
        }
        if(amountValid <= 0) return -1
        var ranValue = ran.nextInt(amountValid)
        for (i in validOptions.indices) {
            if (ranValue == 0) return i
            if (validOptions[i]) ranValue--
        }
        return -1
    }

}
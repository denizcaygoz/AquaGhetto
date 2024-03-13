package service.aiServices.random

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.PrisonBus
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.RootService
import java.util.Random

/**
 * Service class providing basic functions for executing a turn of a random AI
 *
 * @param rootService instance of the [RootService] for access to other services
 * @param randomAIService the random AI service
 */
class RandomAIActionService(private val rootService: RootService, private val randomAIService: RandomAIService) {

    private val ran = Random()

    /**
     * Function that places a tile on a possible prison bus, the bus is selected randomly
     *
     * @param canTakeBuses a list of possible buses
     * @param game the current instance of AquaGhetto
     */
    fun addTileToPrisonBus(canTakeBuses: List<PrisonBus>, game: AquaGhetto) {
        val tileToPlace = if (game.drawStack.isNotEmpty()) game.drawStack.pop() else game.finalStack.pop()
        val busToPlaceOn = canTakeBuses[ran.nextInt(canTakeBuses.size)]
        rootService.playerActionService.addTileToPrisonBus(tileToPlace, busToPlaceOn)
    }

    /**
     * Function to place a prisoner from your own isolation
     * on the game board, the position is selected randomly
     *
     * @param player the current player
     */
    fun moveOwnPrisonerFromIsolation(player: Player) {
        val tileToPlace = player.isolation.peek()
        val validLocations = randomAIService.randomAICheckValidService.validPlaces(tileToPlace, player).toList()
        val location = validLocations[ran.nextInt(validLocations.size)]
        val bonus = rootService.playerActionService.movePrisonerToPrisonYard(location.first , location.second)
        if (bonus.first) {
            placeTile(GuardTile() , player)
        }
        val bonusTile = bonus.second
        if (bonusTile != null) {
            this.placeTile(bonusTile , player)
        }
    }

    /**
     * Function for moving a random prisoner to a random location
     *
     * @param player the current player
     */
    fun moveEmployee(player: Player) {
        /*creates a list of booleans where an employee is*/
        val validOptionsEmployeeFrom = mutableListOf(
            player.hasJanitor,
            player.secretaryCount >= 1, player.secretaryCount >= 2,
            player.lawyerCount >= 1, player.lawyerCount >= 2,
        )
        for (guard in player.board.guardPosition) {
            validOptionsEmployeeFrom.add(true)
        }

        val optionFrom = randomAIService.getRandomValidOption(validOptionsEmployeeFrom)
        val locationRemove: Pair<Int, Int>
        when (optionFrom) {
            0 -> { /*janitor*/
                locationRemove = Pair(-104,-104)
            }
            1,2 -> { /*secretary*/
                locationRemove = Pair(-103,-103)
            }
            3,4 -> { /*lawyer*/
                locationRemove = Pair(-105,-105)
            } else -> {
                val guardToRemove = player.board.guardPosition[optionFrom - 5]
                locationRemove = Pair(guardToRemove.first , guardToRemove.second)
            }
        }

        /*creates a list of booleans where an employee can be placed*/
        val validOptionsEmployeeTo = mutableListOf(
            !player.hasJanitor,
            player.secretaryCount < 1, player.secretaryCount < 2,
            player.lawyerCount < 1, player.lawyerCount < 2,
        )
        val validGuardLocations = randomAIService.randomAICheckValidService.validPlacesGuard(player).toList()
        for (guard in validGuardLocations) {
            validOptionsEmployeeTo.add(true)
        }

        val optionTo = randomAIService.getRandomValidOption(validOptionsEmployeeTo)
        val locationToPlace: Pair<Int, Int>
        when (optionTo) {
            0 -> { /*janitor*/
                locationToPlace = Pair(-104,-104)
            }
            1,2 -> { /*secretary*/
                locationToPlace = Pair(-103,-103)
            }
            3,4 -> { /*lawyer*/
                locationToPlace = Pair(-105,-105)
            } else -> {
                val guardToPlace = validGuardLocations[optionTo - 5]
                locationToPlace = Pair(guardToPlace.first , guardToPlace.second)
            }
        }
        rootService.playerActionService.moveEmployee(locationRemove.first, locationRemove.second,
                                                    locationToPlace.first, locationToPlace.second)
    }

    /**
     * Function for buying a prisoner from another random isolation
     * and place it on a random location
     *
     * @param canBuyPrisonerFrom a list of possible players to buy a prisoner from
     * @param player the current player
     */
    fun buyPrisonerFromOtherIsolation(canBuyPrisonerFrom: List<Player> , player: Player) {
        val playerToBuy = canBuyPrisonerFrom[ran.nextInt(canBuyPrisonerFrom.size)]
        val cardToBuy = playerToBuy.isolation.peek()

        val validLocations = randomAIService.randomAICheckValidService.validPlaces(cardToBuy, player).toList()
        val location = validLocations[ran.nextInt(validLocations.size)]

        val bonus =
            rootService.playerActionService.buyPrisonerFromOtherIsolation(playerToBuy, location.first, location.second)

        if (bonus.first) {
            placeTile(GuardTile() , player)
        }
        val bonusTile = bonus.second
        if (bonusTile != null) {
            this.placeTile(bonusTile , player)
        }
    }

    /**
     * Function to free a prisoner from isolation
     */
    fun freePrisonerFromOwnIsolation() {
        rootService.playerActionService.freePrisoner()
    }

    /**
     * Function for randomly expanding the prison grid, the expansion is placed at a random location
     * the size and the rotation is also random
     *
     * @param player the current player
     */
    fun expandPrisonGrid(player: Player) {
        val optionList = mutableListOf<Boolean>((player.remainingSmallExtensions > 0 && player.coins >= 1),
                                                (player.remainingBigExtensions > 0 && player.coins >= 2))
        val option = randomAIService.getRandomValidOption(optionList)
        when (option) {
            0 -> {
                this.placeExtension(player.board, false)
            }
            1 -> {
                this.placeExtension(player.board, true)
            }
        }
    }


    /**
     * Places an extension at a random valid location
     * This function is very expensive and should not get called often
     *
     * @param board the board of a player
     * @param isBig if the tile should be big or small
     */
    private fun placeExtension(board: Board, isBig: Boolean) {
        val validPlacements = mutableListOf<Triple<Int,Int,Int>>()

        /*get all grid locations surrounding the outer grid*/
        val borderTiles = mutableSetOf<Pair<Int,Int>>()
        for (firstIterator in board.getPrisonGridIterator()) {
            for (secondIterator in firstIterator.value) {
                borderTiles.add(Pair(firstIterator.key + 0, secondIterator.key + 1))
                borderTiles.add(Pair(firstIterator.key + 0, secondIterator.key - 1))
                borderTiles.add(Pair(firstIterator.key + 1, secondIterator.key + 0))
                borderTiles.add(Pair(firstIterator.key - 1, secondIterator.key + 0))

                borderTiles.add(Pair(firstIterator.key + 1, secondIterator.key + 1))
                borderTiles.add(Pair(firstIterator.key - 1, secondIterator.key - 1))
                borderTiles.add(Pair(firstIterator.key + 1, secondIterator.key - 1))
                borderTiles.add(Pair(firstIterator.key - 1, secondIterator.key + 1))
            }
        }

        /*iterate through all border tiles*/
        for (location in borderTiles) {
            /*remove locations, where there is a grid*/
            if (board.getPrisonGrid(location.first, location.second)) continue

            /*add grid location if valid*/
            addExtensionPlacesToList(validPlacements, location.first, location.second, isBig)
        }

        /*get a random valid location*/
        val location = validPlacements[ran.nextInt(validPlacements.size)]

        /*place the expansion*/
        rootService.playerActionService.expandPrisonGrid(isBig, location.first, location.second, location.third)
    }

    /**
     * Adds valid locations for placing an extension in the list, the function
     * test every rotation for the provided location and adds them to a list if
     * these arguments would be valid
     *
     * @param list the list in which the locations should be inserted
     * @param x the x-coordinate of the desired placement
     * @param y the y-coordinate of the desired placement
     * @param isBig if the tile should be big or small
     */
    private fun addExtensionPlacesToList(list: MutableList<Triple<Int,Int,Int>>
                                         , x: Int, y: Int, isBig: Boolean) {
        /*basic pre-check*/
        if (x == 0 && y == 0) return
        if (x == 1 && y == 0) return
        if (x == 0 && y == 1) return

        /*all possible rotations*/
        val validRotations = mutableListOf(0,90,180,270)

        /*check every rotation if it is valid*/
        for (rot in validRotations) {
            if (!rootService.validationService.validateExpandPrisonGrid(isBig, x , y , rot)) continue
            list.add(Triple(x,y,rot))
        }

    }

    /**
     * Function for randomly taking a prison bus and placing the tiles at random locations
     *
     * @param canTakeBuses the buses a player can take
     * @param player the current player
     */
    fun takePrisonBus(canTakeBuses: List<PrisonBus>, player: Player) {
        val busToTake = canTakeBuses[ran.nextInt(canTakeBuses.size)]
        rootService.playerActionService.takePrisonBus(busToTake)
        for (tileToTake in busToTake.tiles) {
            if (tileToTake == null) continue
            this.placeTile(tileToTake, player)
        }
    }

    /**
     * Function for randomly placing a tile, this function
     * also places the bonus at a random location
     *
     * @param tile the tile to place
     * @param player the current player
     */
    private fun placeTile(tile: Tile, player: Player) {
        when (tile) {
            is GuardTile -> {
                /*creates a list of booleans where an employee can be placed*/
                val validOptionsEmployeeTo = mutableListOf(
                    !player.hasJanitor,
                    player.secretaryCount < 1, player.secretaryCount < 2,
                    player.lawyerCount < 1, player.lawyerCount < 2,
                )
                val validGuardLocations = randomAIService.randomAICheckValidService.validPlacesGuard(player).toList()
                for (guard in validGuardLocations) {
                    validOptionsEmployeeTo.add(true)
                }

                val optionTo = randomAIService.getRandomValidOption(validOptionsEmployeeTo)
                val locationToPlace: Pair<Int, Int>
                when (optionTo) {
                    0 -> { /*janitor*/
                        locationToPlace = Pair(-104,-104)
                    }
                    1,2 -> { /*secretary*/
                        locationToPlace = Pair(-103,-103)
                    }
                    3,4 -> { /*lawyer*/
                        locationToPlace = Pair(-105,-105)
                    } else -> {
                    val guardToPlace = validGuardLocations[optionTo - 5]
                    locationToPlace = Pair(guardToPlace.first , guardToPlace.second)
                }
                }
                rootService.playerActionService.moveEmployee(-101, -101,
                    locationToPlace.first, locationToPlace.second)
            }
            is PrisonerTile -> {
                val validLocations = randomAIService.randomAICheckValidService.validPlaces(tile, player).toList()
                /*There is a 5% chance the AI will place a tile in the isolation even tho there is a valid location */
                val location = if (validLocations.isEmpty() || (ran.nextInt(100) < 5)) {
                    Pair(-100,-100) /*isolation*/
                } else {
                     validLocations[ran.nextInt(validLocations.size)]
                }
                val bonus = rootService.playerActionService.placePrisoner(tile, location.first, location.second)
                if (bonus.first) {
                    placeTile(GuardTile() , player)
                }
                val bonusTile = bonus.second
                if (bonusTile != null) {
                    this.placeTile(bonusTile , player)
                }
            }
        }

    }

}
package service.aiServices.random

import entity.AquaGhetto
import entity.Player
import entity.PrisonBus
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.RootService
import service.PlayerActionService.*
import java.util.Random

class RandomAIActionService(private val rootService: RootService, private val randomAIService: RandomAIService) {

    private val ran = Random()

    fun addTileToPrisonBus(canTakeBuses: List<PrisonBus>, game: AquaGhetto) {
        val tileToPlace = if (game.drawStack.isNotEmpty()) game.drawStack.pop() else game.finalStack.pop()
        val busToPlaceOn = canTakeBuses[ran.nextInt(canTakeBuses.size)]
        rootService.playerActionService.addTileToPrisonBus(tileToPlace, busToPlaceOn)
    }

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

    fun freePrisonerFromOwnIsolation() {
        rootService.playerActionService.freePrisoner()
    }

    fun expandPrisonGrid() {
        //TODO
    }

    fun takePrisonBus(canTakeBuses: List<PrisonBus>, player: Player) {
        val busToTake = canTakeBuses[ran.nextInt(canTakeBuses.size)]
        rootService.playerActionService.takePrisonBus(busToTake)
        for (tileToTake in busToTake.tiles) {
            if (tileToTake == null) continue
            this.placeTile(tileToTake, player)
        }
    }

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
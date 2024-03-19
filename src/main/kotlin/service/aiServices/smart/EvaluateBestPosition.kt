package service.aiServices.smart

import entity.AquaGhetto
import entity.Player
import entity.aIActions.PlaceCard
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import kotlin.math.abs
import kotlin.math.min

class EvaluateBestPosition(private val smartAI: SmartAI) {

    private val nextTo = mutableListOf(Pair(0,1),Pair(1,0),Pair(0,-1),Pair(-1,0))
    private val specialPos = mutableListOf(Pair(-102,-102),Pair(-103,-103),Pair(-104,-104))
    private val notGreatPositions = mutableListOf(Pair(0,0),Pair(0,1),Pair(1,0))

    /*boolean is if players earns a bonus coin*/
    fun getBestPositions(tileToPlace: PrisonerTile, player: Player, game: AquaGhetto): Pair<PlaceCard, Boolean>? {

        val tileType = tileToPlace.prisonerType

        val bestLocation = getBestLocationPrisoner(tileToPlace, player, game) ?: return null

        var locationFirstEmployee: Pair<Int,Int>? = null
        var locationBaby: Pair<Int,Int>? = null
        var locationSecondEmployee: Pair<Int,Int>? = null
        var coin = false

        /*simulate placement of tile at the location*/
        player.board.setPrisonYard(bestLocation.first, bestLocation.second, tileToPlace)

        val getEmployeeOne = checkShouldGetBonus(player, tileType)
        if (getEmployeeOne.second) {
            locationFirstEmployee = getBestLocationEmployee(player)
            player.board.setPrisonYard(locationFirstEmployee.first, locationFirstEmployee.second, GuardTile())
        }
        if (getEmployeeOne.first) coin = true

        val shouldGetBaby = this.checkBabyNotRemove(player)
        if (shouldGetBaby != null) {
            val babyToPlace = PrisonerTile(-1, PrisonerTrait.BABY, shouldGetBaby.first.prisonerType)
            val bestBabyLocation = getBestLocationPrisoner(tileToPlace, player, game)
            if (bestBabyLocation != null) {

                player.board.setPrisonYard(bestBabyLocation.first, bestBabyLocation.second, babyToPlace)
                val getEmployeeTwo = checkShouldGetBonus(player, tileType)
                if (getEmployeeTwo.second) {
                    locationSecondEmployee = getBestLocationEmployee(player)
                }
                if (getEmployeeTwo.first) coin = true
                player.board.setPrisonYard(bestBabyLocation.first, bestBabyLocation.second, null)

            } else {
                locationBaby = Pair(-100,-100)
            }
        }


        /*undo all actions, to allow AquaGhetto instance to be used further*/
        player.board.setPrisonYard(bestLocation.first, bestLocation.second, null)
        if (locationFirstEmployee != null) {
            player.board.setPrisonYard(locationFirstEmployee.first, locationFirstEmployee.second, null)
        }

        return Pair(PlaceCard(bestLocation, locationBaby, locationFirstEmployee, locationSecondEmployee), coin)
    }

    fun getBestLocationEmployee(player: Player): Pair<Int,Int> {

        val allValidPositions = mutableListOf<Pair<Pair<Int,Int>, Int>>()

        /*add guard positions*/
        for (firstIterator in player.board.getPrisonGridIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = player.board.getPrisonYard(firstIterator.key, secondIterator.key)
                if (tile != null) continue
                val scoreOfPlacement = smartAI.rootService.evaluationService.getExtraPointsForGuard(firstIterator.key,
                                                                    secondIterator.key, player.board)
                allValidPositions.add(Pair(Pair(firstIterator.key, secondIterator.key), scoreOfPlacement))
            }
        }

        /*add special employee positions*/
        for (pos in specialPos) {
            val score = this.checkPossibleBonus(pos.first, pos.second, player)
            allValidPositions.add(Pair(pos,score))
        }

        return allValidPositions.maxBy { it.second }.first
    }

    private fun checkPossibleBonus(x: Int, y: Int, player: Player): Int {
        when (x) {
            -102 -> {
                /*janitor*/
                if (player.hasJanitor) return -1
                return this.getPointsJanitor(player)
            }
            -103 -> {
                /*secretary*/
                if (player.secretaryCount >= 2) return -1
                return this.getPointsSecretary(player)
            }
            -104 -> {
                /*lawyer*/
                if (player.lawyerCount >= 2) return -1
                return getPointsLawyer(player)
            } else -> {
                /*guard*/
                return smartAI.rootService.evaluationService.getExtraPointsForGuard(x, y, player.board)
            }
        }
    }

    private fun getPointsJanitor(player: Player): Int {
        val isolationPrisonerTypes = mutableSetOf<PrisonerType>()
        for (prisonerTile in player.isolation) {
            isolationPrisonerTypes.add(prisonerTile.prisonerType)
        }
        return (isolationPrisonerTypes.size / 2.0).toInt()
    }

    private fun getPointsSecretary(player: Player): Int {
        /*AI should not like placing a secretary it should invest the coins if possible*/
        return (player.coins * (player.secretaryCount + 1) * 0.3).toInt()
    }

    private fun getPointsLawyer(player: Player): Int {
        var points = 0
        for (xIterator in player.board.getPrisonYardIterator()) {
            for (yIterator in xIterator.value) {
                val tile = yIterator.value
                if (tile !is PrisonerTile) continue
                if (tile.prisonerTrait == PrisonerTrait.RICH) {
                    points += (player.lawyerCount + 1)
                }
            }
        }
        return points
    }

    private fun getBestLocationPrisoner(tileToPlace: PrisonerTile, player: Player, game: AquaGhetto): Pair<Int,Int>? {

        val allValidPositions = mutableListOf<Pair<Pair<Int,Int>, Int>>()

        /*get all valid options and check adjacent grid spaces -> more adjacent grid -> more blocked -> not good*/
        for (firstIterator in player.board.getPrisonGridIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = player.board.getPrisonYard(firstIterator.key, secondIterator.key)
                if (tile != null) continue
                val xPos = firstIterator.key
                val yPos = secondIterator.key
                val validPlacement = smartAI.rootService.validationService.validateTilePlacement(tileToPlace, xPos, yPos, game)
                if (validPlacement) {
                    val pos = Pair(xPos, yPos)
                    allValidPositions.add(Pair(pos, getAdjacentGrid(player, xPos, yPos)))
                }
            }
        }

        /*get all positions with the least adjacentGrid spaces*/
        val bestOption = mutableListOf<Pair<Int,Int>>()
        for (i in 1..4) {
            for (pos in allValidPositions) {
                if (pos.second == i) bestOption.add(pos.first)
            }
            if (bestOption.isNotEmpty()) break
        }

        if (bestOption.isEmpty()) return null

        /*get spaces with the maximum distance to other cards*/
        var secondCheckBest = mutableListOf<Pair<Pair<Int,Int>, Int>>()
        for (pos in bestOption) {
            val dist = this.calcDistanceToDifferentType(player, tileToPlace, pos.first, pos.second)
            secondCheckBest.add(Pair(Pair(pos.first,pos.second),dist))
        }
        secondCheckBest.sortByDescending { it.second }
        secondCheckBest = secondCheckBest.subList(0, min(secondCheckBest.size,5))

        /*if prisoner is old last check is ignored*/
        if (tileToPlace.prisonerTrait == PrisonerTrait.OLD) {
            if (secondCheckBest.isNotEmpty()) {
                val best = secondCheckBest.first()
                return Pair(best.first.first, best.first.second)
            } else {
                return null
            }
        }

        /*get position with the most guards*/
        val thirdCheckBest = mutableListOf<Pair<Pair<Int,Int>, Int>>()
        for (pos in secondCheckBest) {
            thirdCheckBest.add(Pair(Pair(pos.first.first,pos.first.second),
                checkForWorkers(player, pos.first.first, pos.first.second)))
        }

        if (thirdCheckBest.isNotEmpty()) {
            val best = thirdCheckBest.maxBy { it.second }
            return Pair(best.first.first, best.first.second)
        } else {
            return null
        }
    }

    private fun checkForWorkers(player: Player, x: Int, y: Int): Int {
        var countWorkers = 0
        for (dX in -1..1) {
            for (dY in -1..1) {
                if (player.board.getPrisonYard(x + dX, y + dY) is GuardTile) countWorkers++
            }
        }
        return countWorkers
    }

    private fun calcDistanceToDifferentType(player: Player, tileToPlace: PrisonerTile, x: Int, y: Int): Int {
        var smallestDistance = Integer.MAX_VALUE
        for (firstIterator in player.board.getPrisonYardIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = secondIterator.value
                if (tile !is PrisonerTile || tile.prisonerType == tileToPlace.prisonerType) continue
                val dist = abs(x - firstIterator.key) + abs(y - secondIterator.key)
                if (dist < smallestDistance) {
                    smallestDistance = dist
                }
            }
        }
        return smallestDistance
    }

    private fun getAdjacentGrid(player: Player, x: Int, y: Int): Int {
        var count = 0
        for (offset in nextTo) {
            if (player.board.getPrisonGrid(x + offset.first, y + offset.second)) count++
            for (notGreatNeighbour in notGreatPositions) {
                if (x + offset.first == notGreatNeighbour.first && y + offset.second == notGreatNeighbour.second) count++
            }
        }
        return count
    }

    fun checkBabyNotRemove(player: Player): Pair<PrisonerTile, PrisonerTile>?{
        /*get breedable prisoners*/
        val foundBreedableMale = mutableMapOf<PrisonerType, PrisonerTile>()
        val foundBreedableFemale = mutableMapOf<PrisonerType, PrisonerTile>()
        val board = player.board
        for (entry1 in board.getPrisonYardIterator()) {
            val secondMap = entry1.value
            for (entry2 in secondMap) {
                val tile = entry2.value
                if (tile !is PrisonerTile) continue
                val trait = tile.prisonerTrait
                val type = tile.prisonerType
                if (trait == PrisonerTrait.MALE && tile.breedable) foundBreedableMale[type] = tile
                if (trait == PrisonerTrait.FEMALE && tile.breedable) foundBreedableFemale[type] = tile
            }
        }

        /*check for breedable prisoners*/
        for (type in PrisonerType.values()) {
            val male: PrisonerTile? = foundBreedableMale[type]
            val female: PrisonerTile? = foundBreedableFemale[type]
            if (male != null && female != null) {
                return Pair(male, female)
            }
        }

        /*no breedable prisoner was found*/
        return null
    }

    private fun checkShouldGetBonus(player: Player, type: PrisonerType): Pair<Boolean,Boolean> {
        val map = smartAI.rootService.evaluationService.getPrisonerTypeCount(player)
        val tCount = (map[type] ?: 0)
        return if (tCount % 3 == 0 && tCount != 0) {
            Pair(true,false) /*first boolean is get coin second is get employee*/
        } else if (tCount % 5 == 0 && tCount != 0) {
            Pair(false,true) /*first boolean is get coin second is get employee*/
        } else {
            Pair(false,false)
        }
    }

}
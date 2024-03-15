package service.aiServices.smart

import entity.Player
import entity.aIActions.PlaceCard
import entity.enums.PrisonerTrait
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import kotlin.math.abs
import kotlin.math.min

class EvaluateBestPosition(private val smartAI: SmartAI) {

    private val nextTo = mutableListOf(Pair(0,1),Pair(1,0),Pair(0,-1),Pair(-1,0))

    fun getBestPositions(tileToPlace: PrisonerTile, player: Player): MutableList<PlaceCard> {

        return mutableListOf()


    }

    private fun getBestLocationPrisoner(tileToPlace: PrisonerTile, player: Player): Pair<Int,Int> {

        val allValidPositions = mutableListOf<Pair<Pair<Int,Int>, Int>>()

        /*get all valid options and check adjacent grid spaces -> more adjacent grid -> more blocked -> not good*/
        for (firstIterator in player.board.getPrisonGridIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = player.board.getPrisonYard(firstIterator.key, secondIterator.key)
                if (tile != null) continue
                if (smartAI.rootService.validationService.validateTilePlacement(tileToPlace,
                        firstIterator.key, secondIterator.key)) {
                    allValidPositions.add(Pair(Pair(firstIterator.key, secondIterator.key),
                        getAdjacentGrid(player, firstIterator.key, secondIterator.key)))
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
            val best = secondCheckBest.first()
            return Pair(best.first.first, best.first.second)
        }

        /*get position with the most guards*/
        val thirdCheckBest = mutableListOf<Pair<Pair<Int,Int>, Int>>()
        for (pos in secondCheckBest) {
            thirdCheckBest.add(Pair(Pair(pos.first.first,pos.first.second),
                checkForWorkers(player, pos.first.first, pos.first.second)))
        }

        val best = thirdCheckBest.maxBy { it.second }
        return Pair(best.first.first, best.first.second)
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
        var largestDistance = Integer.MIN_VALUE
        for (firstIterator in player.board.getPrisonYardIterator()) {
            for (secondIterator in firstIterator.value) {
                val tile = secondIterator.value
                if (tile !is PrisonerTile || tile.prisonerType == tileToPlace.prisonerType) continue
                val dist = abs(x - firstIterator.key) + abs(y - secondIterator.key)
                if (dist > largestDistance) {
                    largestDistance = dist
                }
            }
        }
        return largestDistance
    }

    private fun getAdjacentGrid(player: Player, x: Int, y: Int): Int {
        var count = 0
        for (pos in nextTo) {
            if (!player.board.getPrisonGrid(x,y)) count++
        }
        return count
    }

}
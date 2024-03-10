package service.aiServices.random

import entity.AquaGhetto
import entity.Player
import entity.PrisonBus
import entity.tileTypes.PrisonerTile
import service.RootService

class RandomAICheckValidService(private val rootService: RootService) {

    fun canTakePrisonBus(player: Player, game: AquaGhetto): MutableSet<PrisonBus> {
        if (player.takenBus != null) return mutableSetOf()
        val result = mutableSetOf<PrisonBus>()
        for (bus in game.prisonBuses) {
            for (i in bus.tiles.indices) {
                if (bus.tiles[i] != null) {
                    result.add(bus)
                }
            }
        }
        return result
    }

    fun canPlaceTileOnBus(player: Player, game: AquaGhetto): MutableSet<PrisonBus> {
        if (player.takenBus != null) return mutableSetOf()
        val result = mutableSetOf<PrisonBus>()
        for (bus in game.prisonBuses) {
            for (i in bus.tiles.indices) {
                if (bus.tiles[i] == null && !bus.blockedSlots[i]) {
                    result.add(bus)
                }
            }
        }
        return result
    }

    fun canBuyExpansion(player: Player): Boolean {
        if (player.takenBus != null) return false
        return if (player.coins == 0) {
            false
        } else if (player.coins == 1) {
            player.remainingSmallExtensions > 0
        } else if (player.coins >= 2) {
            player.remainingSmallExtensions > 0 || player.remainingBigExtensions > 0
        } else {
            false
        }
    }

    fun canFreeOwnPrisoner(player: Player): Boolean {
        if (player.takenBus != null) return false
        return player.isolation.isNotEmpty() && player.coins >= 2
    }

    fun canBuyOtherPrisoner(player: Player, game: AquaGhetto): MutableSet<Player> {
        if (player.takenBus != null) return mutableSetOf()
        if (player.coins < 2) return mutableSetOf()
        val hasTileInIsolation = mutableSetOf<Player>()
        for (otherPlayer in game.players) {
            if (otherPlayer == player) continue
            if (otherPlayer.isolation.isNotEmpty()) hasTileInIsolation.add(player)
        }
        return  hasTileInIsolation
    }

    fun validPlaces(prisonerTile: PrisonerTile, player: Player): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        for (xIterator in player.board.getPrisonGridIterator()) {
            for (yIterator in xIterator.value) {
                val valid = rootService.validationService.validateTilePlacement(prisonerTile, xIterator.key, yIterator.key)
                if (valid) result.add(Pair(xIterator.key, yIterator.key))
            }
        }
        return result
    }

    fun canMoveOwnPrisoner(player: Player): Boolean {
        if (player.takenBus != null) return false
        if (player.isolation.isEmpty()) return false
        return this.validPlaces(player.isolation.peek() , player).isNotEmpty()
    }

    fun canMoveEmployee(player: Player): Boolean {
        if (player.takenBus != null) return false
        val validPlacesGuard = this.validPlacesGuard(player)
        var employees = player.lawyerCount + player.secretaryCount + player.board.guardPosition.size
        if (player.hasJanitor) employees++
        if (employees == 0) return false
        var free = validPlacesGuard.size + (2 - player.lawyerCount) + (2 - player.secretaryCount)
        if (!player.hasJanitor) free++
        if (free == 0) return false
        return true
    }

    fun validPlacesGuard(player: Player): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        for (xIterator in player.board.getPrisonGridIterator()) {
            for (yIterator in xIterator.value) {
                val grid = player.board.getPrisonGrid(xIterator.key , yIterator.key)
                val isOccupied = player.board.getPrisonYard(xIterator.key , yIterator.key) != null
                if (grid && !isOccupied) result.add(Pair(xIterator.key, yIterator.key))
            }
        }
        return result
    }

}
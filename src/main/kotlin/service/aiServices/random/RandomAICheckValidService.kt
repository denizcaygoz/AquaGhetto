package service.aiServices.random

import entity.AquaGhetto
import entity.Player
import entity.PrisonBus
import entity.tileTypes.PrisonerTile
import service.RootService

/**
 * Service class providing basic functions to check if a player can execute an action
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class RandomAICheckValidService(private val rootService: RootService) {

    /**
     * Function to obtain a list of buses a player can take
     * is empty if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @param game the current instance of AquaGhetto
     * @return the list of buses a player can take
     */
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

    /**
     * Function to obtain a list of buses on which a player can place a tile
     * is empty if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @param game the current instance of AquaGhetto
     * @return the list of buses on which a player can place a tile
     */
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

    /**
     * Function to check if a player can buy an expansion
     * is false if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @return if the player can buy an expansion
     */
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

    /**
     * Function to check if a player can free a prisoner
     * is false if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @return if the player can free a prisoner
     */
    fun canFreeOwnPrisoner(player: Player): Boolean {
        if (player.takenBus != null) return false
        return player.isolation.isNotEmpty() && player.coins >= 2
    }

    /**
     * Function to obtain a list of players from whom the player can buy a prisoner
     * is empty if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @param game the current instance of AquaGhetto
     * @return the list of players from whom the player can buy a prisoner
     */
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

    /**
     * Function to obtain a set of pairs containing the position, where a player could place a tile
     *
     * @param player the player whose options should be checked
     * @param prisonerTile the specific tile
     * @return a set of locations where the provided tile could be placed
     */
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

    /**
     * Function to check if a player can move a prisoner from his isolation to the prison grid
     * is false if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @return if the player can move a prisoner
     */
    fun canMoveOwnPrisoner(player: Player): Boolean {
        if (player.takenBus != null) return false
        if (player.isolation.isEmpty()) return false
        return this.validPlaces(player.isolation.peek() , player).isNotEmpty()
    }

    /**
     * Function to check if a player can move an employee
     * is false if the player has already taken a bus
     *
     * @param player the player whose options should be checked
     * @return if the player can move an employee
     */
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

    /**
     * Function to obtain a set of pairs containing the position, where a player could place a guard
     *
     * @param player the player whose options should be checked
     * @return  a set of locations where a guard could be placed
     */
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
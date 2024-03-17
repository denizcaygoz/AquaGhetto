package service.aiServices.smart.evaluateActions

import entity.AquaGhetto
import entity.Player
import entity.aIActions.ActionMoveEmployee
import entity.tileTypes.GuardTile
import service.aiServices.smart.SmartAI

class EvaluateMoveEmployeeService(val smartAI: SmartAI) {

    fun getScoreMoveEmployee(game: AquaGhetto, depth: Int, maximize: Int, amountActions: Int): ActionMoveEmployee {
        val player = game.players[game.currentPlayer]

        if (player.coins < 1) return ActionMoveEmployee(false, 0, Pair(0,0), Pair(0,0))

        /*score advantage, source, destination*/
        val moves = mutableListOf<Triple<Int,Pair<Int,Int>,Pair<Int,Int>>>()

        val validPos = mutableListOf(Pair(-102,-102), Pair(-103,-103), Pair(-103,-103))
        validPos.addAll(player.board.guardPosition)
        for (pos in validPos) {
            val result = this.checkEmployee(pos, player) ?: continue
            moves.add(result)
        }

        if (validPos.isEmpty()) return ActionMoveEmployee(false, 0, Pair(0,0), Pair(0,0))

        val best = moves.maxBy { it.first }
        return ActionMoveEmployee(true, best.first, best.second, best.third)
    }

    private fun checkEmployee(pos: Pair<Int,Int>, player: Player): Triple<Int,Pair<Int,Int>,Pair<Int,Int>>? {
        val oldScore = smartAI.evaluateGamePosition.evaluateCurrentPosition()
        player.coins -= 1
        removeEmployee(pos.first, pos.second, player)
        val betterPos = smartAI.evaluateBestPosition.getBestLocationEmployee(player)

        if (betterPos.first != pos.first && betterPos.second != pos.second) {
            player.coins += 1
            addEmployee(pos.first, pos.second, player)
            return null
        }

        addEmployee(betterPos.first, betterPos.second, player)

        //TODO maybe get future score?
        val newScore = smartAI.evaluateGamePosition.evaluateCurrentPosition()

        player.coins += 1
        removeEmployee(betterPos.first, betterPos.second, player)
        addEmployee(pos.first, pos.second, player)


        return if (newScore-1 > oldScore) {
            Triple(newScore, pos, betterPos)
        } else {
            null
        }
    }

    private fun addEmployee(x: Int, y: Int, player:Player) {
        when (x) {
            -102 -> {
                /*janitor*/
                player.hasJanitor = true
            }
            -103 -> {
                /*secretary*/
                player.secretaryCount++
            }
            -104 -> {
                /*lawyer*/
                player.lawyerCount++
            } else -> {
            /*guard*/
            player.board.setPrisonYard(x, y, GuardTile())
            }
        }
    }

    private fun removeEmployee(x: Int, y: Int, player:Player) {
        when (x) {
            -102 -> {
                /*janitor*/
                player.hasJanitor = false
            }
            -103 -> {
                /*secretary*/
                player.secretaryCount--
            }
            -104 -> {
                /*lawyer*/
                player.lawyerCount--
            } else -> {
            /*guard*/
            player.board.setPrisonYard(x, y, null)
        }
        }
    }

}
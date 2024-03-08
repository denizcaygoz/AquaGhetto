package service

import entity.enums.PlayerType
import view.Refreshable

class GameService(private val rootService: RootService): Refreshable {

    fun startNewGame(players: MutableList<Pair<String, PlayerType>>) {

    }

}
package service

import entity.enums.PlayerType
import view.Refreshable

class GameService(private val rootService: RootService): AbstractRefreshingService() {

    fun startNewGame(players: MutableList<Pair<String, PlayerType>>) {

    }

}
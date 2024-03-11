package service

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.enums.PlayerType

class GameService(private val rootService: RootService): AbstractRefreshingService() {

    fun startNewGame(players: MutableList<Pair<String, PlayerType>>) {

        /*checks for a valid player count*/
        require(players.size >= 2) { "minimum 2 players" }
        require(players.size <= 5) { "maximum 5 players" }

        /*check for valid player names (if name exist, no duplicate)*/
        val checkDoublePlayersList = mutableListOf<String>()
        for (player in players) {
            require(player.first.isNotBlank()) {"player name is not allowed to be empty"}
            require(!checkDoublePlayersList.contains(player.first)) {"unique player names required"}
            checkDoublePlayersList.add(player.first)
        }

        /*create game*/
        val game = AquaGhetto()

        /*Create list of players*/
        val playerList = mutableListOf<Player>()
        for (p in players) {
            val player = Player(p.first, p.second)
            playerList.add(player)
            initializeBoard(player.board)
        }
        game.players = playerList

        /*Fills the allTiles list in AquaGhetto with all tiles*/
        rootService.boardService.createAllTiles()

        /*Create the normal and the final draw stack*/
        val bothStacks = rootService.boardService.createStacks(playerList.size)
        game.drawStack = bothStacks.first
        game.finalStack = bothStacks.second

        /*Create prisonBusses*/
        game.prisonBusses = rootService.boardService.createPrisonBusses(playerList.size)

        /*Set current game to newly created game*/
        rootService.currentGame = game

        /*Call refreshes*/
        onAllRefreshables {
            refreshAfterStartGame()
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    /**
     * Initializes the board by adding the "default" 19 spaces
     */
    private fun initializeBoard(board: Board) {
        for (x in 1..4) {
            for (y in 1..4) {
                if (x == 4 && y == 4) continue
                board.setPrisonGrid(x, y, true)
            }
        }
        board.setPrisonGrid(0, 2, true)
        board.setPrisonGrid(0, 3, true)
        board.setPrisonGrid(2, 0, true)
        board.setPrisonGrid(3, 0, true)
    }

}
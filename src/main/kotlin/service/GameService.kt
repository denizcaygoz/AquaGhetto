package service

import entity.AquaGhetto
import entity.Board
import entity.Player
import entity.PrisonBus
import entity.enums.PlayerType
import service.networkService.ConnectionState
import tools.aqua.bgw.core.BoardGameApplication
import kotlin.concurrent.thread

/**
 * Service layer class that provides the logic for basic game functions, like creating a new game or
 * determine the next player
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class GameService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Starts a new game
     *
     * Function used to start a new game by creating a new instance of AquaGhetto
     * Creates a list of players bases on the argument and sets the list of players in AquaGhetto to
     * this list. Every board of a player is initialized with the default prison yard.
     * The list of all tiles is created and the normal drawStack and the finalStack is created
     * The list of prison buses is also created
     * At the end rootService.currentGame is set to the newly created game
     * refreshAfterStartGame and refreshAfterNextTurn with the current player is called
     */
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

        /*Set current game to newly created game*/
        rootService.currentGame = game

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
        game.prisonBuses = rootService.boardService.createPrisonBuses(playerList.size)

        /*Call refreshes*/
        onAllRefreshables {
            refreshAfterStartGame()
            refreshAfterNextTurn(game.players[game.currentPlayer])
            refreshPrisonBus(null)
        }

        /*if the first player is an AI call the AI service*/
        //this.checkAITurn(game.players[game.currentPlayer], 1500)
    }

    /**
     * Function to determine the next player
     *
     * This function is used to set game.currentPlayer to the "next" player based on the amount of
     * taken buses
     * If a card was taken from the finalStack and all players have taken a
     * bus the game will end, evaluateGame is called
     *
     * @param busWasTakenInThisRound Whether a [PrisonBus] was taken in this Round.
     */
    fun determineNextPlayer(busWasTakenInThisRound: Boolean) {
        // Copy current game and use it as a new starting point
        val game = rootService.gameStatesService.copyAquaGhetto()
        val currentPlayer = game.players[game.currentPlayer]
        val busTaken = currentPlayer.takenBus
        val state = rootService.networkService.connectionState
        rootService.currentGame = game
        game.nextState = null /*ensure the next state is null and redo is not possible*/

        val isTwoPlayerGame = game.players.size == 2

        val numberOfBussesLeft = if (isTwoPlayerGame) {
            game.players.count { it.takenBus == null }
        } else {
            game.prisonBuses.size
        }

        onAllRefreshables { refreshPrisonBus(null) }

        when (numberOfBussesLeft) {
            1 ->  {
                // Without that, it would still be the second-to-last player's turn
                if (busWasTakenInThisRound) {
                    do {
                        game.currentPlayer = (game.currentPlayer + 1) % game.players.size
                    } while (game.players[game.currentPlayer].takenBus != null)
                }

                game.players[game.currentPlayer].currentScore = rootService.evaluationService.evaluatePlayer(game.players[game.currentPlayer])
                onAllRefreshables {
                    refreshScoreStats()
                    refreshAfterNextTurn(game.players[game.currentPlayer])
                }
                this.checkAITurn(game.players[game.currentPlayer])
            }
            0 -> { /*all players have taken a buss*/
                if (game.finalStack.size != 15) { /*reserve stack was taken*/
                    rootService.evaluationService.evaluateGame()
                    Thread.sleep(5000)
                    onAllRefreshables {
                        refreshAfterEndGame()
                    }
                } else {
                    /*reserve stack was not taken*/
                    /*next player is the current player*/
                    game.players[game.currentPlayer].currentScore = rootService.evaluationService.evaluatePlayer(game.players[game.currentPlayer])
                    onAllRefreshables {
                        refreshScoreStats()
                    }
                    startNewRound(game)
                }
            }
            else -> {
                /*
                sets the current player to the next player
                provided that they didn't take a bus
                */

                do {
                    game.currentPlayer = (game.currentPlayer + 1) % game.players.size
                } while (game.players[game.currentPlayer].takenBus != null)

                game.players[game.currentPlayer].currentScore = rootService.evaluationService.evaluatePlayer(game.players[game.currentPlayer])
                onAllRefreshables {
                    refreshScoreStats()
                    refreshAfterNextTurn(game.players[game.currentPlayer])
                }
                this.checkAITurn(game.players[game.currentPlayer])
            }
        }

        if (busWasTakenInThisRound && busTaken != null && state == ConnectionState.PLAYING_MY_TURN) {
            rootService.networkService.sendTakeTruck(busTaken.index)
        }

    }

    /**
     * Function to start a new round
     *
     * This function is used to start a new round, all prisonBusses
     * are placed in the middle
     */
    private fun startNewRound(game: AquaGhetto) {
        for (player in game.players) {
            val bus = player.takenBus
            checkNotNull(bus) {"Not all players have taken a bus"}
            game.prisonBuses.add(bus)
            player.takenBus = null
        }
        for (bus in game.prisonBuses) {
            bus.tiles = Array(3) {null}
        }

        onAllRefreshables {
            refreshPrisonBus(null)
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
        this.checkAITurn(game.players[game.currentPlayer])
    }

    /**
     * If the provided player is an AI calls makeTurn in AIService
     *
     * @param player the player in whose name the action is performed
     * @param delay not used anymore, still there to not break anything
     * @see AIService
     */
    fun checkAITurn(player: Player, delay: Int = 0) {
        val state = rootService.networkService.connectionState
        val isNetworkGame = state != ConnectionState.DISCONNECTED

        if (!isNetworkGame) {
            innerCheckAITurn(player)
        } else {
            if (state == ConnectionState.PLAYING_MY_TURN) {
                innerCheckAITurn(player)
            }
        }

    }

    /**
     * inner function of checkAITurn
     *  If the provided player is an AI calls makeTurn in AIService
     *
     * @param player the player in whose name the action is performed
     * @param delay not used anymore, still there to not break anything
     * @see AIService
     **/
    private fun innerCheckAITurn(player: Player) {
        require(player.takenBus == null)
        if (player.type == PlayerType.AI || player.type == PlayerType.RANDOM_AI) {
            thread {
                Thread.sleep(500)
                BoardGameApplication.runOnGUIThread {
                    rootService.aiService.makeTurn(player)
                }
                //rootService.aiService.makeTurn(player)
            }
        }
    }

    /**
     * Initializes the board by adding the "default" 19 spaces
     * @param board the board to initialize
     */
    fun initializeBoard(board: Board) {
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
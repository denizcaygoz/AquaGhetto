package service

import entity.AquaGhetto
import entity.Player
import entity.enums.PlayerType

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

        /*Create list of players*/
        val playerList = mutableListOf<Player>()
        for (p in players) {
            val player = Player(p.first, p.second)
            playerList.add(player)
        }
        game.players = playerList

        /*Fills the allTiles list in AquaGhetto with all tiles*/
        rootService.boardService.createAllTiles()

        /*Create the normal and the final draw stack*/
        val bothStacks = rootService.boardService.createStacks(playerList.size)
        game.drawStack = bothStacks.first
        game.finalStack = bothStacks.second

        /*Create prisonBusses*/
        game.prisonBusses = rootService.boardService.createPrisonBuses(playerList.size)

        /*Set current game to newly created game*/
        rootService.currentGame = game

        /*Call refreshes*/
        onAllRefreshables {
            refreshAfterStartGame()
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

    /**
     * Function to determine the next player
     *
     * This function is used to set game.currentPlayer to the "next" player based on the amount of
     * taken buses
     * If a card was taken from the finalStack and all players have taken a
     * bus the game will end, evaluateGame is called
     */
    fun determineNextPlayer() {
        val game = rootService.currentGame
        checkNotNull(game) { "No running game." }
        var amountTakenBusses = 0
        for (player in game.players) {
            if (player.takenBus == null) {
                amountTakenBusses++
            }
        }

        when (amountTakenBusses) {
            1 ->  {
                onAllRefreshables {
                    refreshAfterNextTurn(game.players[game.currentPlayer])
                }
            }
            0 -> { /*all players have taken a buss*/
                if (game.finalStack.size != 15) { /*reserve stack was taken*/
                    rootService.evaluationService.evaluateGame()
                } else {
                    /*reserve stack was not taken*/
                    /*next player is the current player*/
                    startNewRound(game)
                }
            }
            else -> { /*sets the current player to the next player*/
                game.currentPlayer = (game.currentPlayer + 1) % game.players.size
                onAllRefreshables {
                    refreshAfterNextTurn(game.players[game.currentPlayer])
                }
            }
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
            game.prisonBusses.add(bus)
            onAllRefreshables {
                refreshPrisonBus(bus)
            }
            player.takenBus = null
        }
        onAllRefreshables {
            refreshAfterNextTurn(game.players[game.currentPlayer])
        }
    }

}
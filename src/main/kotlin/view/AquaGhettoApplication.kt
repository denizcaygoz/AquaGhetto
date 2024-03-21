package view


import entity.Player
import service.RootService
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.event.KeyCode

/**
 * Application and SceneManagement
 * Needs to be adjusted Down the Line if something is missing
 */
class AquaGhettoApplication: BoardGameApplication("AquaGhetto"), Refreshable {


    /**
     * Central Service to provide all Game/PlayerActions
     */
    private val rootService = RootService()

    /**
     * Main in game Scene
     * this will need an Esc Key Listener to go to Pause Menu
     */
    private var inGameScene = InGameScene(rootService).apply {
        onKeyPressed = {
            if (it.keyCode == KeyCode.ESCAPE) { this@AquaGhettoApplication.showMenuScene(pauseMenuScene)}
        }
    }

    /**
     * The Main Menu Screen for selecting a mode
     */
    private val mainMenuScene = MainMenuScene(rootService).apply {

        quitButton.onMouseClicked = { exit() }

        //This needs to reference the Name of the Button on the MainMenuScene that starts a new Game
        newSinglePlayerGameButton.onMouseClicked = {
            this@AquaGhettoApplication.showMenuScene(setupScene)
        }

        /**
        * the newMultiplayerGameButton creates a new Game
        * if need be [setupScene] can be duplicated to display the Lobbycode in there
        */
        hostButton.onMouseClicked = {
            this@AquaGhettoApplication.showMenuScene(setupScene)
        }

        /**
         * the join button get the Lobbycode out of [lobbycodeInputfield]
        */
        joinButton.onMouseClicked = {
        this@AquaGhettoApplication.showMenuScene(setupScene)
        }
    }

    /**
     * The PausePage that opens up by pressing Esc while being in the In game
     */
    private val pauseMenuScene = PauseMenuScene(rootService).apply {
        onKeyPressed = {
            if (it.keyCode == KeyCode.ESCAPE) {
                this@AquaGhettoApplication.hideMenuScene(500 )
            }
        }
        resumeGameButton.onMouseClicked = { this@AquaGhettoApplication.hideMenuScene( 500 ) }

        //maybe this needs to be coded in the scene and not here?
        undoButton.onMouseClicked = { rootService.gameStatesService.undo() }
        redoButton.onMouseClicked = { rootService.gameStatesService.redo() }
        saveButton.onMouseClicked = { rootService.gameStatesService.saveGame() }
        loadButton.onMouseClicked = { rootService.gameStatesService.loadGame() }
    }

    /**
     * The Final Scene After the Game that shows the Scoreboard of Players
     */
    private val scoreboardScene = ScoreboardScene(rootService, inGameScene).apply {
        backToMenuButton.onMouseClicked = {
            this@AquaGhettoApplication.hideMenuScene()
            this@AquaGhettoApplication.showMenuScene(mainMenuScene)
        }
    }

    /**
     * The Scene for setting up a new Game
     */
    private val setupScene = SetupScene(rootService).apply {
        startNewGameButton.onMouseClicked = {

            this@AquaGhettoApplication.hideMenuScene(500)
            this@AquaGhettoApplication.showGameScene(inGameScene)
        }
    }

    init{
        //we dont have a "addRefreshables" function so everything needs to be added individually

        setupScene.startNewGameButton.apply {
            onMouseClicked = {
                rootService.gameService.startNewGame(setupScene.getPlayerList(setupScene.testCheck.isSelected))
                val game = rootService.currentGame
                checkNotNull(game)
                game.delayTime = setupScene.delayInputPlayer1.text.toInt()
                //game.delayTime = 2000
                showGameScene(inGameScene)
                rootService.gameService.checkAITurn(game.players[game.currentPlayer])
            }
        }

        mainMenuScene.hostButton.apply {
            onMouseClicked = {
                rootService.networkService.startNewHostedGame()
                showGameScene(inGameScene)
            }
        }

        rootService.addRefreshables(
            this,
            inGameScene,
            mainMenuScene,
            pauseMenuScene,
            pauseMenuScene,
            setupScene
        )

        //this is from NetWar
        onWindowClosed = {
            rootService.networkService.disconnect()
        }
        this.showMenuScene(mainMenuScene)
    }

    override fun refreshAfterStartGame() {
        this.hideMenuScene()
    }

    override fun refreshAfterNextTurn(player: Player) {
        this.showGameScene(inGameScene)
    }

    override fun refreshAfterEndGame() {
        this.showMenuScene(scoreboardScene)
    }
}
package view


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
        rootService.addRefreshable(this)
        rootService.addRefreshable(inGameScene)
        rootService.addRefreshable(mainMenuScene)
        rootService.addRefreshable(pauseMenuScene)
        rootService.addRefreshable(scoreboardScene)
        rootService.addRefreshable(setupScene)

        //this is from NetWar
        onWindowClosed = {
            rootService.networkService.disconnect()
        }
        this.showMenuScene(mainMenuScene)
    }

    override fun refreshAfterStartGame() {
        this.hideMenuScene()
    }

    override fun refreshAfterEndGame() {
        this.showMenuScene(scoreboardScene)
    }
}
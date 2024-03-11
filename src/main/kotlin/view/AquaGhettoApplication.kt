package view

import service.RootService
import tools.aqua.bgw.core.BoardGameApplication

/**
 * Application and SceneManagement
 * Needs to be adjusted Down the Line if something is missing
 */
class AquaGhettoApplication: BoardGameApplication("AquaGhetto"), Refreshable

/**
 * Central Service to provide all Game/PlayerActions
 */
private val rootService = RootService()

/**
 * Main Ingame Scene
 * this will need a Esc Key Listener to go to Pause Menu
 */
private val inGameScene = InGameScene(rootService).apply {
    onKeyPressed = {
        if (it.keyCode.isEscape) { this@AquaGhettoApplication.showMenuScene(pauseMenuScene)}
    }
}

/**
 * The Main Menu Screen for selecting a mode
 */
private val mainMenuScene = MainMenuScene(rootService).apply {

    quitButton.onMouseClicked = { exit() }

    //This needs to refrence the Name of the Button on the MainMenuScene that starts a new Game
    newSingleplayerGameButton.onMouseClicked = {
        this@AquaGhettoApplication.showMenuScene(setupScene)
    }

    /*this is a Placeholder for a Button that starts or Joins a Multiplayer Game
    For the Time being the Multiplayer Button will refer to Single Player Setup */
    newMultiplayerGameButton.onMouseClicked = {
        this@AquaGhettoApplication.showMenuScene(setupScene)
    }
}

/**
 * The PausePage that opens up by pressing Esc while being in the Ingame
 */
private val pauseMenuScene = PauseMenuScene(rootService).apply {

    //maybe this needs to be coded in the scene and not here?
    undoButton.onMouseClicked = { rootService.gameStatesService.undo() }
    redoButton.onMouseClicked = { rootService.gameStatesService.redo() }
    saveButton.onMouseClicked = { rootService.gameStatesService.saveGame() }
    loadButton.onMouseClicked = { rootService.gameStatesService.loadGame() }
}

/**
 * The Final Scene After the Game that shows the Scoreboard of Players
 */
private val scoreboardScene = ScoreboardScene(rootService)

/**
 * The Scene for setting up a new Game
 */
private val setupScene = SetupScene(rootService)

package view

import entity.enums.PlayerType
import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.MenuScene

/**
 * Sceen for Selecting a Gameplay mode
 * Contains: Singleplayerbutton, MultiplayerButton
 * Multiplayer Prompts the User to Create or Join a Game
 * Join creates a TextField Input for Lobbycode and a Button to join
 * Create goes to the setupScene
 * -> Maybe the Create a Game needs a Specialized Scene that Displays the Lobbycode given by Network?
 */
class MainMenuScene(rootService : RootService, test:SceneTest3) : MenuScene(), Refreshable {

    /**
     * Exits the Game
     * Logic of the Button is in [AquaGhettoApplication]
     */
    val quitButton = Button(
        posX = (1920 / 2)-250 ,
        posY = 1080-200,
        height = 100,
        width = 500,
        text = "Quit the Game"
    )

    /**
     * Switches to [SetupScene] to create a Singleplayer Game
     */
    val newSinglePlayerGameButton = Button(
        posX = (1920 / 2)-250,
        posY = 300,
        height = 100,
        width = 500,
        text = "Singleplayer"
    )

    /**
     * Creates 2 buttons onclick
     * [newMultiplayerGameButton] to Create a Game
     * [joinMultiplayerButton] to join a Game
     */
    val multiplayerButton = Button(
        posX = (1920 / 2)-250,
        posY = 450,
        height = 100,
        width = 500,
        text = "Multiplayer"
    ).apply {
        onMouseClicked = {
                this.isVisible = false
                this.isFocusable = false
            val newMultiplayerGameButton = Button(
                posX = (1920 / 2)-250,
                posY = 450,
                height = 100,
                width = 225,
                text = "Create"
            )
            val joinMultiplayerButton = Button(
                posX = (1920 / 2)+25,
                posY = 450,
                height = 100,
                width = 225,
                text = "Join",
            ).apply {
                onMouseClicked = {
                    this.isVisible = false
                    this.isFocusable = false
                    newMultiplayerGameButton.width = 100.0
                    val lobbycodeInputfield = TextField(
                        posX = (1920 / 2)-100,
                        posY = 450,
                        height = 100,
                        width = 250,
                        text = "LobbyCode"
                    ).apply {
                        onMouseClicked = {
                            text = ""
                        }
                    }
                    val joinButton = Button(
                        posX = (1920 / 2)+150,
                        posY = 450,
                        height = 100,
                        width = 100,
                        text = "Go!"
                    )
                    addComponents( lobbycodeInputfield , joinButton )
                }
            }
        addComponents( newMultiplayerGameButton , joinMultiplayerButton )
        }
    }
    
    init {
        addComponents(
            quitButton,
            newSinglePlayerGameButton,
            multiplayerButton,
            )
    }
}
fun main() {
    val test = SceneTest3()
    test.show()
}
class SceneTest3 : BoardGameApplication("Test") , Refreshable {
    private val rootService = RootService()

    private val setupScene = MainMenuScene(rootService, this)

    init {
        rootService.addRefreshables(this,setupScene)
        rootService.gameService.startNewGame(
            mutableListOf(Pair("Moin", PlayerType.PLAYER), Pair("Moin2", PlayerType.PLAYER)))
        this.showMenuScene(setupScene)
    }
}
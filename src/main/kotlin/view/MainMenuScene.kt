package view

import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.MenuScene

/**
 * Sceen for Selecting a Gameplay mode
 * Contains: Singleplayerbutton, MultiplayerButton
 * Multiplayer Prompts the User to Create or Join a Game
 * Join creates a TextField Input for Lobbycode and a Button to join
 * Create goes to the setupScene
 * -> Maybe the Create a Game needs a Specialized Scene that Displays the Lobbycode given by Network?
 */
class MainMenuScene(rootService : RootService) : MenuScene(), Refreshable {

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
        posY = 500,
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
        posY = 700,
        height = 100,
        width = 500,
        text = "Multiplayer"
    ).apply {
        onMouseClicked = {
                this.isVisible = false
                this.isFocusable = false
            val newMultiplayerGameButton = Button(
                posX = (1920 / 2)-250,
                posY = 700,
                height = 100,
                width = 200,
                text = "Create"
            )
            val joinMultiplayerButton = Button(
                posX = (1920 / 2)+50,
                posY = 700,
                height = 100,
                width = 200,
                text = "Join",
            ).apply {
                onMouseClicked = {
                    val lobbycodeInputfield = TextField(
                        posX = (1920 / 2)+50,
                        posY = 800,
                        height = 100,
                        width = 150,
                        text = "LobbyCode"
                    )
                    val joinButton = Button(
                        posX = (1920 / 2)+50,
                        posY = 950,
                        height = 50,
                        width = 50,
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
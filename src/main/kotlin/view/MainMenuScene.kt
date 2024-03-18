package view

import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.MenuScene

/**
 * Buttons from Application
 * quitButton
 * newSingleplayerGameButton
 * newMultiplayerGameButton
 *
 */
class MainMenuScene(rootService : RootService) : MenuScene(), Refreshable {

    val quitButton = Button(
        posX = (1920 / 2)-250 ,
        posY = 1080-200,
        height = 100,
        width = 500,
        text = "Quit the Game"
    )
    val newSinglePlayerGameButton = Button(
        posX = (1920 / 2)-250,
        posY = 500,
        height = 100,
        width = 500,
        text = "Singleplayer"
    )
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
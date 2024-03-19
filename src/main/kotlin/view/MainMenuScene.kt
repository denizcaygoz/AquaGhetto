package view

import entity.enums.PlayerType
import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
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
class MainMenuScene(rootService : RootService, test:SceneTest3 = SceneTest3()) : MenuScene(), Refreshable {

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
    val hostButton = Button(
        posX = (1920 / 2) -250,
        posY = 750,
        height = 100,
        width = 500,
        text = "Create Game"
    ).apply {
        isVisible = false
        onMouseClicked = {
            rootService.networkService.startNewHostedGame()
        }
    }
    val joinButton = Button(
        posX = (1920 / 2) -250,
        posY = 750,
        height = 100,
        width = 500,
        text = "Go!"
    ).apply { isVisible = false }

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
            )
            val lobbycodeInputfield = TextField(
                posX = (1920 / 2)-250,
                posY = 600,
                height = 100,
                width = 225,
                text = "LobbyCode"
            ).apply { isVisible = false }
            val nameGuestInputField = TextField(
                posX = (1920 / 2) + 25,
                posY = 600,
                height = 100,
                width = 225,
                text = "Name"
            ).apply { isVisible = false }
            val joinButton = Button(
                posX = (1920 / 2) -250,
                posY = 750,
                height = 100,
                width = 500,
                text = "Go!"
            ).apply {
                isVisible = false
                onMouseClicked = {
                    val guestName = nameGuestInputField.text
                    val sessionID = lobbycodeInputfield.text
                    rootService.networkService.joinGame("aqua24a", guestName, sessionID)
                    newMultiplayerGameButton.isDisabled = true

                }
            }

            val nameHostInputField = TextField(
                posX = (1920 / 2) -250,
                posY = 600,
                height = 100,
                width = 325,
                text = "Name"
            ).apply { isVisible = false }
            val sessionLabel = Label(
                posX = (1920 / 2),
                posY = 550,
                height = 100,
                width = 225,
                text = ""
            ).apply { isVisible = false }
           /* val hostButton = Button(
                posX = (1920 / 2) -250,
                posY = 750,
                height = 100,
                width = 500,
                text = "Create Game"
            ).apply {
                isVisible = false
                onMouseClicked = {
                    rootService.networkService.startNewHostedGame()
                }
            }*/
            val lobbyButton = Button(
                posX = (1920 / 2) + 125,
                posY = 600,
                height = 100,
                width = 125,
                text = "Open Lobby"
            ).apply {
                isVisible = false
                onMouseClicked = {
                    val hostName = nameHostInputField.text
                    rootService.networkService.hostGame("aqua24a", hostName, "")
                    val sessionID = rootService.networkService.createadSessionID
                    requireNotNull(sessionID) { "No sessionID was created." }
                    nameHostInputField.isDisabled = true
                    joinMultiplayerButton.isDisabled = true
                    newMultiplayerGameButton.isDisabled = true
                    hostButton.isVisible = true
                    nameHostInputField.text = "SessionID: "+sessionID
                    nameHostInputField.width = 500.0
                    this.isVisible = false
                }
            }

            joinMultiplayerButton.apply {
                onMouseClicked = {
                    lobbycodeInputfield.isVisible = true
                    nameGuestInputField.isVisible = true
                    joinButton.isVisible = true
                    sessionLabel.isVisible = false
                    hostButton.isVisible = false
                    lobbyButton.isVisible = false
                    nameHostInputField.isVisible = false
                }
            }

            newMultiplayerGameButton.apply {
                onMouseClicked = {
                    lobbycodeInputfield.isVisible = false
                    nameGuestInputField.isVisible = false
                    joinButton.isVisible = false
                    sessionLabel.isVisible = true
                    hostButton.isVisible = false
                    lobbyButton.isVisible = true
                    nameHostInputField.isVisible = true
                    nameHostInputField.apply {
                        onMouseClicked = {
                            text = ""
                        }
                    }
                }
            }
        addComponents(
            newMultiplayerGameButton,
            joinMultiplayerButton,
            nameHostInputField,
            nameGuestInputField,
            hostButton,
            sessionLabel,
            lobbyButton,
            lobbycodeInputfield,
            joinButton
            )
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
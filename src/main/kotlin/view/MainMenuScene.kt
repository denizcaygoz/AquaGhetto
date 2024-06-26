package view

import entity.enums.PlayerType
import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ImageVisual
import java.awt.Color
import java.awt.Image

/**
 * Sceen for Selecting a Gameplay mode
 * Contains: Singleplayerbutton, MultiplayerButton
 * Multiplayer Prompts the User to Create or Join a Game
 * Join creates a TextField Input for Lobbycode and a Button to join
 * Create goes to the setupScene
 * -> Maybe the Create a Game needs a Specialized Scene that Displays the Lobbycode given by Network?
 */
class MainMenuScene(rootService : RootService) : MenuScene(), Refreshable {
    private val backgroundLabel = Label(
        posY = 0, posX = 0, width = 1920, height = 1080, visual = ImageVisual("background/SetupBackground.png") )
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
                    rootService.networkService.joinGame("aqua24a", guestName, sessionID, getPlayerType(typeSelector))
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
           val hostButton = Button(
                posX = (1920 / 2) -250,
                posY = 750,
                height = 100,
                width = 500,
                text = "Create Game"
            ).apply {
                isVisible = false
                onMouseClicked = {
                    rootService.networkService.startNewHostedGame(getPlayerType(typeSelector))
                }
            }
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
                    typeSelector.isVisible = true
                    playertypeLabel.isVisible = true
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
                    typeSelector.isVisible = true
                    playertypeLabel.isVisible = true
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



        val typeSelector = Button(
            posX = (1920 / 2) + 250,
            posY = 600,
            width = 100,
            height = 100,
            text = "PLAYER",
            font = Font(size = 1),
            alignment = Alignment.CENTER,
            visual = ImageVisual("icon/PLAYER.png"),
        ).apply {
            this.isVisible = false
            onMouseClicked = {
                when (text) {
                    "PLAYER" -> {
                        text = "AI";
                        visual = ImageVisual("icon/AI.png");
                        //delayInputPlayer1.apply { delayInputPlayer1.isVisible = true }
                    }

                    "AI" -> {
                        text = "RANDOM";
                        visual = ImageVisual("icon/RANDOM.png");
                        //delayInputPlayer1.apply { delayInputPlayer1.isVisible = true }
                    }

                    "RANDOM" -> {
                        text = "PLAYER";
                        visual = ImageVisual("icon/PLAYER.png");

                        //delayInputPlayer1.apply { delayInputPlayer1.isVisible = false }
                    }

                    else -> {
                        text = "icon/PLAYER";
                        visual = ImageVisual("PLAYER.png")
                        //delayInputPlayer1.apply { delayInputPlayer1.isVisible = false }
                    }
                }
                refreshLabel()
            }
        }
    fun getPlayerType(button: UIComponent?): PlayerType {
        var selectedPlayerType: PlayerType = PlayerType.PLAYER
        if (button is Button) {
            when (button.text) {
                "PLAYER" -> selectedPlayerType = PlayerType.PLAYER
                "AI" -> selectedPlayerType = PlayerType.AI
                "RANDOM" -> selectedPlayerType = PlayerType.RANDOM_AI
                "NETWORK" -> selectedPlayerType = PlayerType.NETWORK
                else -> selectedPlayerType = PlayerType.PLAYER
            }
        }
        return selectedPlayerType
    }
    val playertypeLabel : Label = Label(
        font = Font(color = Color.YELLOW), text = getPlayerType(typeSelector).toString()).apply { isVisible = false }
    private fun refreshLabel() : Unit{
        playertypeLabel.apply { text = getPlayerType(typeSelector).toString() }
    }
    init {
        addComponents(
            quitButton,
            newSinglePlayerGameButton,
            multiplayerButton,
            typeSelector,
            playertypeLabel
            )
        background = ImageVisual("background/MainBackground.png")    }
}

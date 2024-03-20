package view

import entity.Player
import entity.enums.PlayerType
import org.w3c.dom.Text
import service.RootService
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.*
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.event.KeyCode
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import java.awt.Checkbox
import java.awt.Color
import java.awt.Image
import java.lang.reflect.Type
import javax.imageio.ImageIO

/**
 * Our Setup side that generates a game based on user input
 * uses a Grid [playerGrid] to Organise the Labels and Buttons
 * Grid gets filled with Elements
 * First Colum is for TextInput field
 * Second Colum is for PlayerType Switcher
 * Third Colum is to delete that Colum
 * Buttons Should know their Position in the Grid due to Parameter Position
 * if this is not the Case text can be used to save position
 */
class SetupScene (rootService : RootService, test: SceneTest2 = SceneTest2()) : MenuScene(1920 , 1080 ), Refreshable {
    /**
     * CheckBox that lets the Player toggle on a Random order for the PlayerList
     * CheckBox changes its state upon click
     *  ToggleButton might be suited better
     */
    val testCheck =
        ToggleButton(posX = 400, posY = (1080 - 200), text = "Normal Order", width = 400,).apply {
            onMouseClicked = {
                //this.isSelected = !this.isSelected
                if (this.isSelected) {
                    text = "Random Order"
                } else {
                    text = "Normal Order"
                }
            }
        }

    /*Grid to Display PlayerCards*/
    private val playerGrid: GridPane<UIComponent> =
        GridPane(posX = 550, posY = 450, columns = 3, rows = 5, spacing = 25)

    /**
     * User can Set the Delay in s
     * Default is 5s
     */
    val delayInputPlayer1: TextField = TextField(
        posX = 400,
        posY = (1080 - 250),
        height = 50,
        width = 50,
        text = "5",
    ).apply {
        this.isVisible = false
        onKeyTyped = {
            if(it.keyCode.isLetter()){
                this.text = ""
            }
        }
    }
    private fun determineAIPlayer(): Unit {
        var shouldDelayInputbeVisible : Boolean = false
        for (i in 0 until playerGrid.rows) {

            if(getPlayerType(playerGrid[1, i]) == PlayerType.RANDOM_AI ||
                getPlayerType(playerGrid[1, i]) == PlayerType.AI)
            {
                shouldDelayInputbeVisible = true;
            }
        }
        delayInputPlayer1.apply { isVisible = shouldDelayInputbeVisible}
    }
        /**
         * Creates a TextField were the User can Input a PlayerName
         * Length of the input is unrestricted
         * @param textInput String that gets put into text of the TextField
         */
        private fun createPlayerInputfield(textInput: String): TextField {
            val textField = TextField(
                posY = 0,
                posX = 100,
                width = 300,
                height = 50,
                font = Font(20, Color.BLACK),
                text = textInput,
            ).apply {
                /*Check if Startnewgame should be disabled with every Keypress
            * StartNewGameButton is Enabled from the Start so Going with Default Names works*/
                onKeyTyped = {
                    if (determinePlayerCount() < 2 || determinePlayerCount() > 5) {
                        startNewGameButton.isDisabled = true
                    } else {
                        startNewGameButton.isDisabled = false
                    }
                    //Limit for InputLength could be added here
                }
            }

            return textField
        }


        /**
         * creates a Button that onCLick adds Inputfield TypeSelector and Remove Button to a Row in the grid
         * does the Opposite as RemovePlayerButton
         * @return Button
         */
        private fun createPlayerAddButton(position: Int): Button {
            val addButton = Button(
                posX = 100,
                posY = 0,
                width = 50,
                height = 50,
                text = "add",
                alignment = Alignment.CENTER,
                visual = ImageVisual("icon/ADD.png")
            ).apply {
                onMouseClicked = {
                    playerGrid[0, position] = createPlayerInputfield("Player${position + 1}")
                    playerGrid[1, position] = createPlayerTypeSelector(position)
                    playerGrid[2, position] = createRemovePlayerButton(position)
                }
            }
            return addButton
        }

        /**
         * creates a button that will remove Player Creation Buttons from the grid at its position
         * will also delete itself
         * @return button
         */
        private fun createRemovePlayerButton(position: Int): Button {
            val removeButton = Button(
                posX = 0,
                posY = 0,
                width = 50,
                height = 50,
                text = "remove",
                alignment = Alignment.CENTER_LEFT,
                visual = ImageVisual("icon/remove.png"),
            ).apply {
                onMouseClicked = {
                    //remove
                    playerGrid[0, position] = createPlayerAddButton(position)
                    playerGrid[1, position] = null
                    playerGrid[2, position] = null

                    if (determinePlayerCount() >= 2) {
                        startNewGameButton.isDisabled = true
                    }
                }
            }
            return removeButton
        }

        /**
         * creates a Button that can switch from one Playertype to another
         * State is saved in text with Fontsize 0
         * @return Button
         */
        private fun createPlayerTypeSelector(position: Int): Button {
            val typeSelector = Button(
                posX = 0,
                posY = 0,
                width = 50,
                height = 50,
                text = "PLAYER",
                font = Font(size = 0),
                alignment = Alignment.CENTER,
                visual = ImageVisual("icon/PLAYER.png"),
            ).apply {
                /*Unsure if we should be able to swap a Player to a Network Player
            * Network case can be removed in that case*/
                onMouseClicked = {
                    when (text) {
                        "PLAYER" -> {
                            text = "AI";
                            visual = ImageVisual("icon/AI.png");
                            determineAIPlayer()
                            //delayInputPlayer1.apply { delayInputPlayer1.isVisible = true }
                        }

                        "AI" -> {
                            text = "RANDOM";
                            visual = ImageVisual("icon/RANDOM.png");
                            determineAIPlayer()
                            //delayInputPlayer1.apply { delayInputPlayer1.isVisible = true }
                        }

                        "RANDOM" -> {
                            text = "NETWORK";
                            visual = ImageVisual("icon/NETWORK.png");
                            determineAIPlayer()

                            //delayInputPlayer1.apply { delayInputPlayer1.isVisible = false }
                        }

                        "NETWORK" -> {
                            text = "PLAYER";
                            visual = ImageVisual("icon/PLAYER.png");
                            determineAIPlayer()
                            //delayInputPlayer1.apply { delayInputPlayer1.isVisible = false }
                        }

                        else -> {
                            text = "icon/PLAYER";
                            visual = ImageVisual("PLAYER.png")
                            determineAIPlayer()
                            //delayInputPlayer1.apply { delayInputPlayer1.isVisible = false }
                        }
                    }
                }
            }
            return typeSelector
        }

        /**This Button Needs
         * a [getPlayerList] Function
         * to validate amount of Players with Names [determinePlayerCount]
         */
        val startNewGameButton = Button(
            posX = 400,
            posY = (1080 - 150),
            height = 50,
            width = 200,
            text = "Start New Game",
            alignment = Alignment.CENTER,
            visual = ColorVisual(Color.YELLOW)
        ).apply {
            onMouseClicked = {
                rootService.gameService.startNewGame(getPlayerList(testCheck.isSelected))
                val game = rootService.currentGame
                checkNotNull(game)
                game.delayTime = delayInputPlayer1.text.toInt()
            }
        }

        /**
         * Determines the PlayerType of the Playertype Button
         * @param button that needs to be determined
         * @return PlayerType of button
         */
        private fun getPlayerType(button: UIComponent?): PlayerType {
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

        /**
         *  Creates a List of Players [playerList] for the Start game button
         *  DONE: needs to acutally shuffle the List if [random] == true
         *  @return List with <Player>
         */
        private fun getPlayerList(random: Boolean): MutableList<Pair<String, PlayerType>> {
            val playerList: MutableList<Pair<String, PlayerType>> = mutableListOf()
            for (i in 0 until playerGrid.rows) {
                val current = if (playerGrid[0, i] != null) {
                    playerGrid[0, i]
                } else {
                    continue
                }
                if (current is TextField && current.text.isNotEmpty()) {
                    playerList.add(Pair(current.text, getPlayerType(playerGrid[1, i])))
                }
            }
            if (random) {
                playerList.shuffle()
            }
            return playerList
        }

        /**
         * gives the amount of Players currently set by the User
         * @retun an Int Value
         */
        private fun determinePlayerCount(): Int {
            var playerCount: Int = 0
            for (i in 0 until playerGrid.rows) {
                val current = if (playerGrid[0, i] != null) {
                    playerGrid[0, i]
                } else {
                    continue
                }
                if (current is TextField && current.text.isNotEmpty()) {
                    playerCount++
                }
            }
            return playerCount
        }

        init {
            addComponents(

                testCheck,
                playerGrid,
                startNewGameButton,
                delayInputPlayer1,

            )
            playerGrid.setColumnWidth(0, 300)
            playerGrid.setColumnWidth(1, 50)
            playerGrid.setColumnWidth(2, 50)
            playerGrid.setCenterMode(Alignment.CENTER)


            playerGrid[0, 0] = createPlayerInputfield("Player1")
            playerGrid[0, 1] = createPlayerInputfield("Player2")
            playerGrid[1, 0] = createPlayerTypeSelector(0)
            playerGrid[1, 1] = createPlayerTypeSelector(1)
            playerGrid[0, 2] = createPlayerAddButton(2)
            playerGrid[0, 3] = createPlayerAddButton(3)
            playerGrid[0, 4] = createPlayerAddButton(4)
            background = ImageVisual("background/SetupBackground.png")

        }
    }


/*Fails to build in Bgw? :(((*/
fun main() {
    val test = SceneTest2()
    test.show()
}
class SceneTest2 : BoardGameApplication("Test") , Refreshable {
    private val rootService = RootService()

    private val setupScene = SetupScene(rootService, this)

    init {
        rootService.addRefreshables(this,setupScene)
        rootService.gameService.startNewGame(
            mutableListOf(Pair("Moin", PlayerType.PLAYER), Pair("Moin2", PlayerType.PLAYER)))
        this.showMenuScene(setupScene)


    }
}

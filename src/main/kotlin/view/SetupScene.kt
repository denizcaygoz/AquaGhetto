package view

import entity.Player
import entity.enums.PlayerType
import service.RootService
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.*
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.MenuScene
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
 * Grid gets filled with Elemts
 * First Colum is for TextInput field
 * Second Colum is for PlayerType Switcher
 * Third Colum is to delete that Colum
 */
class SetupScene (rootService : RootService, test: SceneTest2) : MenuScene(1920 , 1080 ), Refreshable {
    //idk
    val testCheck = CheckBox(posX = 100, posY = 100,text = "Random order?" ).apply {
        onMouseClicked = {
            this.isChecked = !this.isChecked
        }
    }

    /*Grid to Display PlayerCards*/
    private val playerGrid: GridPane<UIComponent> = GridPane(posX = 600, posY =450, columns = 3, rows = 5)

    private fun createPlayerInputfield(textInput: String): TextField {
        val textField = TextField(
            posY = 0,
            posX = 100,
            width = 500,
            height = 50,
            font = Font(40 , Color.WHITE ),
            text = textInput,
            prompt = "PlayaNameHereUwU"
        )
        /*Logic what it actually does is missing*/
        return textField
    }
   private fun createPlayerAddButton(position : Int) : Button {
       val addButton = Button(
           posX = 100,
           posY = 0,
           width = 50,
           height = 50,
           text = "add",
           alignment = Alignment.CENTER,
           visual = ImageVisual("Test.png")
       )
       /*Button Logic missing*/
       return addButton
   }

    private fun createRemovePlayerButton(position: Int): Button {
        val removeButton = Button(
            posX = 100,
            posY = 0    ,
            width = 50,
            height = 50,
            text = "remove",
            alignment = Alignment.CENTER_LEFT ,
            visual = ImageVisual("Test.png"),
        )
        /*Button Logic missing*/
        return removeButton
    }

    private fun createPlayerTypeSelector() : Button {
        val typeSelector = Button (
            posX = 100,
            posY = 0,
            width = 50,
            height = 50,
            text = "PLAYER",
            font = Font(size = 0),
            alignment = Alignment.CENTER,
            visual = ImageVisual("PLAYER.png"),
        ).apply {
           onMouseClicked = {
               when(text){
                   "PLAYER" -> { text = "AI" ; visual = ImageVisual("AI.png") }
                   "AI" -> { text = "RANDOM" ; visual = ImageVisual("RANDOM.png") }
                   "RANDOM" -> { text = "NETWORK" ; visual = ImageVisual("NETWORK.png") }
                   "NETWORK" -> { text = "PLAYER"; visual = ImageVisual("PLAYER.png") }
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
        posX = ((1920/2)-100),
        posY = (1080-150),
        height = 50,
        width = 200,
        text = "Start New Game",
        alignment = Alignment.CENTER,
        visual = ImageVisual("Test.png"),
    ).apply {
        onMouseClicked = {
            rootService.gameService.startNewGame(getPlayerList(testCheck.isChecked))
        }
    }

    private fun getPlayerType(button : UIComponent?) : PlayerType{
        var selectedPlayerType: PlayerType = PlayerType.PLAYER
        if(button is Button) {
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
     *  @return List with <Player>
    */
    private fun getPlayerList(random : Boolean) : MutableList<Pair<String,PlayerType>> {
        val playerList: MutableList<Pair<String,PlayerType>> = mutableListOf()
        for (i in 0 until playerGrid.rows) {
            val current = if (playerGrid[0, i] != null) {
                playerGrid[0, i]
            } else {
                continue
            }
            if (current is TextField && current.text.isNotEmpty() ) {
                playerList.add( Pair( current.text , getPlayerType( playerGrid[1,i] )))
            }
        }
        return playerList
    }

    /**
     * gives the amount of Players currently set by the User
     * @retun an Int Value
     */
    private fun determinePlayerCount() : Int{
        var playerCount : Int = 0
        for ( i in 0 until playerGrid.rows)
        {
            val current = if(playerGrid[0,i] != null)
            {
                playerGrid[0,i]
            }else
            {
                continue
            }
            if(current is TextField && current.text.isNotEmpty())
            {
                playerCount++
            }
        }
        return playerCount
    }



    init {
        addComponents(
            testCheck,
            playerGrid,
        )
        playerGrid.setRowHeights(100)
        playerGrid.setCenterMode(Alignment.CENTER_LEFT)

        playerGrid[0,0] = createPlayerInputfield("Player1")
        playerGrid[0,1] = createPlayerInputfield("Player2")
        playerGrid[1,0] = createPlayerTypeSelector()
        playerGrid[1,1] = createPlayerTypeSelector()
        playerGrid[0,2] = createPlayerAddButton(2)
        playerGrid[0,3] = createPlayerAddButton(3)
        playerGrid[0,4] = createPlayerAddButton(4)
    }
}

fun main() {
    val test = SceneTest2()
    test.show()
}
class SceneTest2 : BoardGameApplication("Test") , Refreshable {
    private val rootService = RootService()

    private val setupScene = SetupScene(rootService, this)

    init {
        rootService.addRefreshables(this,setupScene)
        rootService.gameService.startNewGame(mutableListOf(Pair("Moin", PlayerType.PLAYER), Pair("Moin2", PlayerType.PLAYER)))
        this.showMenuScene(setupScene)
    }
}

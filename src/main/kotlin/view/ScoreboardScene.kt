package view

import entity.enums.PlayerType
import service.RootService
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.*
import tools.aqua.bgw.core.*
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import java.awt.Color
import java.util.concurrent.Delayed

class ScoreboardScene (rootService : RootService) : MenuScene(1920, 1090) , Refreshable{

    private val playerLabel = Label(
        posX =  (1920/2) - (400/2),
        posY = 0,
        width = 400,
        height = 200,
        alignment = Alignment.CENTER,
        text = "Test",
        font = Font(size = 100)
    )
    val backToMenuButton = Button(
        posX = (1920/2) - (200/2),
        posY = (1090 - 50),
        width = 200,
        height = 50,
        text = "Back to Main Menu",
        font = Font (size = 50),
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color.YELLOW)/*Piss Yellow for testing*/
    ).apply {
        this.onMouseEntered = {
            visual = ColorVisual(Color.RED)
        }
        this.onMouseExited = {
            visual = ColorVisual(Color.YELLOW)

        }
    }
    val scoreboardGrid : GridPane<ComponentView> = generateGridPane()
    private fun generateGridPane() : GridPane<ComponentView>{
        /*checkNotNull(rootService.currentGame)
        val game = rootService.currentGame
        val scoreboardGrid = GridPane<ComponentView>((1920/2),(1080/2),3,5)
        val rankedList = game.evaluateGame()
        for(i in rankedList.indices)
        {
            /*Placeholder this needs to generate Labels with informations of certain size
            * currently this would do nothing because the Grid needs to contain UI Elements!*/
            val forPlayer = rankedList[i]
            /*scoreboardGrid[0,i] = forPlayer.name
            scoreboardGrid[1,i] = forPlayer.points
            scoreboardGrid[2,i] = forPlayer.getPrisonyard*/
        }*/
        return scoreboardGrid
    }
}
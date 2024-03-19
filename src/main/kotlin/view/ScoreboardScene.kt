package view

import entity.Player
import entity.enums.PlayerType
import service.*
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

class ScoreboardScene (val rootService : RootService) : MenuScene(1920, 1080) , Refreshable {

    val root = rootService
    private fun createTextLabel( text : String ) : Label {
         val playerLabel = Label(
            posX = 0,
            posY = 0,
            width = 200,
            height = 100,
            alignment = Alignment.CENTER,
            text = text,
            font = Font(size = 30)
        )
        return playerLabel
    }
    private fun createPrisonYard(player : Player) : InGameScene.PlayerBoard
    {
        return InGameScene.PlayerBoard(player, rootService)
    }

    val backToMenuButton = Button(
        posX = (1920/2) - (200/2),
        posY = (1080 - 50),
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

    /**
     * root.EvaluationService doesnt return a MutableList thats Sorted by Player Position
     * So this Function needs to sort the Players
     * Addidionally
     */
     fun generateGridPane() : GridPane<UIComponent>{
        val scoreboardGrid = GridPane<UIComponent>((1920/2),(1080/2),3,5, spacing = 50)
        val game = root.currentGame
        checkNotNull(game)
        val rankedList  = game.players
        rankedList.sortBy { it.currentScore }
        for(i in rankedList.indices) {

            val forPlayer : Player = rankedList[i]
            scoreboardGrid[0, i] = createTextLabel(forPlayer.name)
            scoreboardGrid[1, i] = createTextLabel(root.evaluationService.evaluatePlayer(forPlayer).toString())
            scoreboardGrid[2, i] = null

        /*createPrisonYard(forPlayer)*/

        }
        return scoreboardGrid
    }

    val scoreboardGrid : GridPane<UIComponent> = generateGridPane()

    init {
        addComponents(
            scoreboardGrid ,
            backToMenuButton,
            )
        scoreboardGrid.setRowHeights(100) // size of a Prison Grid


    }
}
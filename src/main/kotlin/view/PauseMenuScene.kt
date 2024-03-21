package view

import service.RootService
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.visual.ImageVisual

/**
 * this should get a grid aswell for the Buttons to be centered
 * maybe we want 2 rows?
 *  Undo Redo in the same row?
 *  Resume Game Button would be a great addition to Pressing ESC?
 */
class PauseMenuScene (rootService : RootService) : MenuScene(1920,1080), Refreshable{
    private val pauseGrid = GridPane<Button>(
        posX = (1920/2) ,
        posY = (1080/2) ,
        3,
        3 ,
        spacing = 10,

    )
    val pausedIndicator : Label = Label(posY = 1080-200 , posX = 1920 / 2 , width = 200, height = 200, text = "Safran")
    fun refreshpauseIndicator(rootService: RootService){
        pausedIndicator.apply { text = rootService.currentlyonPause.toString() }
    }
    /*Action Handling is currently in AquaGhettoApplication*/
    val resumeGameButton = Button(posX = (1920/2)-200, posY = 25, width = 400, height = 100, text = "Resume Game")
    val undoButton = Button(posX = 0, posY = 0,width = 400, height = 100, text = "Undo").apply {
        onMouseClicked = {
            rootService.gameStatesService.undo()
        }
    }
    val redoButton = Button(posX = 0, posY = 0,width = 400, height = 100, text = "Redo").apply {
        onMouseClicked = {
            rootService.gameStatesService.redo()
        }
    }
    val saveButton = Button(posX = 0, posY = 0,width = 400, height = 100, text = "Save").apply {
        onMouseClicked = {
            rootService.gameStatesService.saveGame()
        }
    }
    val loadButton = Button(posX = 0, posY = 0, width = 400, height = 100, text = "Load").apply {
        onMouseClicked = {
            rootService.gameStatesService.loadGame()
        }
    }

    init {

        pauseGrid.setCenterMode(Alignment.CENTER)


        /*Not sure if resume Button will span both Rows if not needs to be created outside*/
        pauseGrid[1,0] = resumeGameButton
        pauseGrid[0,1] = undoButton
        pauseGrid[2,1] = redoButton
        pauseGrid[0,2] = saveButton
        pauseGrid[2,2] = loadButton

        addComponents(pauseGrid,pausedIndicator)
        refreshpauseIndicator(rootService)

        background = ImageVisual("background/MainBackground.png")
    }



}
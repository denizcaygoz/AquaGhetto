package view

import service.RootService
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.MenuScene

/**
 * this should get a grid aswell for the Buttons to be centered
 * maybe we want 2 rows?
 *  Undo Redo in the same row?
 *  Resume Game Button would be a great addition to Pressing ESC?
 */
class PauseMenuScene (rootService : RootService) : MenuScene(1920,1080), Refreshable{

    private val pauseGrid = GridPane<ComponentView>(
        (1920/2)-250 ,
        (1080/2)-300 ,
        3 ,
        2 ,
        spacing = 100,
    )

    /*Action Handling is currently in AquaGhettoApplication*/
    val resumeGameButton = Button(posX = 100, posY = 100, width = 450, height = 100, text = "Resume Game")
    val undoButton = Button(posX = 200, posY = 200,width = 400, height = 100, text = "Undo").apply {
        onMouseClicked = {
            rootService.gameStatesService.undo()
        }
    }
    val redoButton = Button(posX = 300, posY = 300,width = 400, height = 100, text = "Redo").apply {
        onMouseClicked = {
            rootService.gameStatesService.redo()
        }
    }
    val saveButton = Button(posX = 400, posY = 400,width = 400, height = 100, text = "Save").apply {
        onMouseClicked = {
            rootService.gameStatesService.saveGame()
        }
    }
    val loadButton = Button(posX = 500, posY = 500, width = 400, height = 100, text = "Load").apply {
        onMouseClicked = {
            rootService.gameStatesService.loadGame()
        }
    }

    init {
        addComponents(resumeGameButton,redoButton,undoButton,saveButton,loadButton)
        pauseGrid.setRowHeights(50)
        pauseGrid.setCenterMode(Alignment.CENTER)
        pauseGrid.setCellCenterMode(0 , 0 ,  Alignment.TOP_CENTER)

        /*
        /*Not sure if resume Button will span both Rows if not needs to be created outside*/
        pauseGrid[0,0] = resumeGameButton
        pauseGrid[1,0] = undoButton
        pauseGrid[1,1] = redoButton
        pauseGrid[2,0] = saveButton
        pauseGrid[2,1] = loadButton*/

    }



}
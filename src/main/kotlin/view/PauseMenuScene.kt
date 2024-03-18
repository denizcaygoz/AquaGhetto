package view

import service.RootService
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene

/**
 * this should get a grid aswell for the Buttons to be centered
 * maybe we want 2 rows?
 *  Undo Redo in the same row?
 *  Resume Game Button would be a great addition to Pressing ESC?
 */
class PauseMenuScene (rootService : RootService) : MenuScene(1920,1080), Refreshable{

    val pauseGrid = GridPane<ComponentView>(1920,1080,3,2)

    /*Action Handling is currently in AquaGhettoApplication*/
    val resumeGameButton = Button(width = 400, height = 100, text = "Resume Game")
    val undoButton = Button(width = 400, height = 100, text = "Undo")
    val redoButton = Button(width = 400, height = 100, text = "Redo")
    val saveButton = Button(width = 400, height = 100, text = "Save")
    val loadButton = Button(width = 400, height = 100, text = "Load")

    init {
        /*Buttons dont do anything jet*/
        pauseGrid[0,0] = resumeGameButton
        pauseGrid[1,0] = undoButton
        pauseGrid[1,1] = redoButton
        pauseGrid[2,0] = saveButton
        pauseGrid[2,1] = loadButton

    }



}
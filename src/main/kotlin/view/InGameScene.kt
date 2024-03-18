package view

import entity.enums.PlayerType
import service.RootService
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.event.KeyCode
import tools.aqua.bgw.components.layoutviews.CameraPane
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.Visual
import java.awt.Color

class InGameScene(rootService: RootService, test: SceneTest) : BoardGameScene(1920,1080), Refreshable {

    private val testButton = Button(100,100,100,100)
    private val testButton2 = Button(100,100,100,100, visual = ColorVisual.PINK)

    // Test Grid
    private val testPrison = PlayerBoard()

    // Camera Pane stuff
    private val targetLayout = Pane<ComponentView>(width = 1920, height = 1080, visual = ImageVisual("Test_BackGround_Ingame.png"))
    private val cameraPane = CameraPane(width = 1920, height = 1080, target = targetLayout, visual = ColorVisual.BLUE).apply {
        isHorizontalLocked = false
        isVerticalLocked = false
        isZoomLocked = false
    }


    init {
        background = ImageVisual("Test_BackGround_Ingame.png")

        onKeyPressed = { event ->
            if (event.keyCode == KeyCode.A) {
                println("A was pressed")

                // Enable interactivity for zooming and panning
                //cameraPane.interactive = true

                // Zoom in by setting the zoom factor
                cameraPane.zoom = 2.0 // Example zoom factor of 2

                // Pan the camera to specific coordinates
                cameraPane.pan(100, 100, smooth = true) // Example coordinates

            }
        }
        targetLayout.addAll(
            testButton,
            testPrison
        )

        // Add the cameraPane to the scene
        addComponents(cameraPane)
    }
}

class PlayerBoard() : GridPane<Button>(rows = 21, columns = 21, layoutFromCenter = false) {

    init {
        this.posX = 100.0
        this.posY = 100.0

        for (y in 0 until this.rows) {
            for (x in 0 until this.columns) {
                val tempText : String = ""
                this[x,y] = Button(height = 50, width = 50, text = "$y, $x", visual = Visual.EMPTY)
                this.spacing = 1.0
                //this[x,y].apply {on}
            }
        }
        for(y in 9..11) {
            for(x in 8..12) {
                this[x,y]?.visual = ImageVisual("tiles/default_tile.png")
            }
        }
        for(x in 9..11) {
            this[x,8]?.visual = ImageVisual("tiles/default_tile.png")
            this[x,12]?.visual = ImageVisual("tiles/default_tile.png")
        }
    }

    /**
     * Transforms coordinates from the service layer
     * to coordinates
     */
    fun coords(serviceCoord : Int) : Int{
        return -(serviceCoord - 21)
    }
}

/**
 * Below this are methods for testing the IngameScene
 */
fun main() {
    val test = SceneTest()
    test.show()
}

class SceneTest : BoardGameApplication("AquaGhetto"), Refreshable {
    private val rootService = RootService()
    private val gameScene = InGameScene(rootService, this)

    init {
        rootService.gameService.startNewGame(mutableListOf(Pair("Moin", PlayerType.PLAYER), Pair("Moin2", PlayerType.PLAYER)))
        showGameScene(gameScene)
        show()
    }
}

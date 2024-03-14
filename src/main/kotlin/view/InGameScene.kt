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
import java.awt.Color

class InGameScene(rootService: RootService, test: SceneTest) : BoardGameScene(1920,1080), Refreshable {

    private val testButton = Button(100,100,100,100)
    private val testButton2 = Button(100,100,100,100, visual = ColorVisual.PINK)

    // Test Grid
    private var prisonPlayer1 = GridPane<Button>(400,400,5,5)

    // Camera Pane stuff
    private val targetLayout = Pane<ComponentView>(width = 1920, height = 1080, visual = ImageVisual("Test_BackGround_Ingame.png"))
    private val cameraPane = CameraPane(width = 1920, height = 1080, target = targetLayout, visual = ColorVisual.BLUE).apply {
        isHorizontalLocked = false
        isVerticalLocked = false
        isZoomLocked = false
    }


    init {
        background = ImageVisual("Test_BackGround_Ingame.png")

        for (x in 0.. prisonPlayer1.rows) {
            for (y in 0.. prisonPlayer1.columns) {

            }
        }
        prisonPlayer1[0,1] = Button()
        //prisonPlayer1[-1,0] = Button()

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
            testButton
        )

        // Add the cameraPane to the scene
        addComponents(
            cameraPane,
            prisonPlayer1,
            testButton2)
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

package view

import entity.Player
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
import kotlin.math.absoluteValue

class InGameScene(var rootService: RootService, test: SceneTest) : BoardGameScene(1920,1080), Refreshable {

    private val testButton = Button(100,100,100,100)
    private val testButton2 = Button(100,100,100,100, visual = ColorVisual.PINK)

    // Grids
    private val prisons : MutableList<PlayerBoard> = mutableListOf()

    // Camera Pane stuff
    private val targetLayout = Pane<ComponentView>(width = 1920, height = 1080, visual = ColorVisual.RED)
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
            testButton
        )

        // Add the cameraPane to the scene
        addComponents(cameraPane)
    }

    override fun refreshAfterStartGame() {
        val game = rootService.currentGame
        checkNotNull(game) {"There is no game running"}
        val playerCount = game.players.size

        for(i in 0 until playerCount) {
            prisons.add(PlayerBoard(game.players[i]))
        }

        if (playerCount == 5) {
            prisons[0].apply{
                posX = 960.0
                posY = 850.0
            }
            prisons[1].apply{
                posX = 230.0
                posY = 540.0
            }
            prisons[2].apply{
                posX = 480.0
                posY = 230.0
            }
            prisons[3].apply{
                posX = 1440.0
                posY = 230.0
            }
            prisons[4].apply{
                posX = 1690.0
                posY = 540.0
            }
        }
        targetLayout.addAll(prisons)
    }
}

class PlayerBoard(player: Player) : GridPane<Button>(rows = 21, columns = 21, layoutFromCenter = true) {

    init {
        this.posX = 100.0
        this.posY = 100.0
        this.spacing = 1.0

        /**
        for (y in 0 until this.rows) {
            for (x in 0 until this.columns) {
                this[x,y] = Button(height = 50, width = 50, visual = Visual.EMPTY).apply {
                    this.isDisabled = true
                    this.isVisible = false
                    //text = "$y, $x"
                }
            }
        }
        */

        for(y in 9..11) {
            for(x in 8..12) {
                this[x,y] = Button(height = 50, width = 50, visual = Visual.EMPTY).apply{
                    this.visual = ImageVisual("tiles/default_tile.png")
                    this.isDisabled = false
                    this.isVisible = true
                }
            }
        }
        for(x in 9..11) {
            this[x,8] = Button(height = 50, width = 50, visual = Visual.EMPTY).apply{
                this.visual = ImageVisual("tiles/default_tile.png")
                this.isDisabled = false
                this.isVisible = true
            }
            this[x,12] = Button(height = 50, width = 50, visual = Visual.EMPTY).apply{
                this.visual = ImageVisual("tiles/default_tile.png")
                this.isDisabled = false
                this.isVisible = true
            }
        }
    }

    /**
     * Transforms coordinates from the service layer
     * to coordinates
     */
    fun coords(serviceCoord : Int) : Int{
        return -(serviceCoord - 21)
    }

    /**
     * Returns the size, which the prison has to have on screen.
     * 1.0 is full size
     * 0.5 is half size and so on
     */
    fun calculateSize(player: Player) : Double {
        // Save the span of the grid first
        var maxX = -20
        var minX = 20
        var maxY = -20
        var minY = 20
        for ((x, innerMap) in player.board.getPrisonGridIterator()) {
            for ((y, value) in innerMap) {
                if (value) { // If the grid exists
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        val importantNumber =
        if ((maxX.absoluteValue-minX.absoluteValue) >= (maxY.absoluteValue-minY.absoluteValue)) {
            (maxX.absoluteValue-minX.absoluteValue)
        } else (maxY.absoluteValue-minY.absoluteValue)

        if(importantNumber <= 7) return 1.0
        else if(importantNumber <= 9) return 0.8
        else return 0.5

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
        rootService.gameService.startNewGame(mutableListOf(
            Pair("Moin0", PlayerType.PLAYER),
            Pair("Moin1", PlayerType.PLAYER),
            Pair("Moin2", PlayerType.PLAYER),
            Pair("Moin3", PlayerType.PLAYER),
            Pair("Moin4", PlayerType.PLAYER)))
        gameScene.refreshAfterStartGame()
        showGameScene(gameScene)
    }
}

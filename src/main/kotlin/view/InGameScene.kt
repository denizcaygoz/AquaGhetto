package view

import entity.Player
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
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
                posX = 430.0
                posY = 540.0
            }
            prisons[2].apply{
                posX = 680.0
                posY = 230.0
            }
            prisons[3].apply{
                posX = 1240.0
                posY = 230.0
            }
            prisons[4].apply{
                posX = 1490.0
                posY = 540.0
            }
        }

        /*
        PLAYER POSITIONING FOR ALL PLAYER COUNTS TO BE ADDED
         */

        targetLayout.addAll(prisons)
    }
}

class PlayerBoard(val player: Player) : GridPane<Button>(rows = 21, columns = 21, layoutFromCenter = true) {

    var currentGridSize = calculateSize()
    //var isolation = player.

    init {
        this.posX = 100.0
        this.posY = 100.0
        this.spacing = 1.0*currentGridSize

        // Iterator for grid
        val gridIterator = player.board.getPrisonGridIterator()
        while (gridIterator.hasNext()) {
            val entry = gridIterator.next()
            val x = entry.key
            val yMap = entry.value

            for ((y, floor) in yMap) {
                if(floor) {
                    val tempX = coordsToView(x,y).first
                    val tempY = coordsToView(x,y).second
                    this[tempX, tempY] = Button(height = 50*currentGridSize, width = 50*currentGridSize).apply{
                        this.visual = ImageVisual("tiles/default_tile.png")
                    }
                }
            }
        }
        // Iterator for Yard
        val yardIterator = player.board.getPrisonYardIterator()
        while (yardIterator.hasNext()) {
            val entry = yardIterator.next()
            val x = entry.key
            val yMap = entry.value

            for ((y) in yMap) {
                val slot = this[coordsToView(x,y).first, coordsToView(x,y).second]
                checkNotNull(slot) {"There is no Yard at the given coordinates x:$x and y: $y (View cords)"}
                slot.apply{
                    this.visual = tileVisual(player.board.getPrisonYard(x,y) as PrisonerTile)
                }
            }
        }

    }

    // Assisting methods from here on

    /**
     * Transforms coordinates from the service layer
     * to view coordinates
     */
    fun coordsToView(serviceX : Int, serviceY : Int) : Pair<Int,Int>{
        return Pair(serviceX+10 , -serviceY+10)
    }

    /**
     * Transforms coordinates from the view layer
     * to service layer coordinates
     */
    fun coordsToService(viewX : Int, viewY : Int) : Pair<Int,Int>{
        return Pair(viewX-10 , -viewY+10)
    }

    /**
     * Returns the size, which the prison has to have on screen.
     * 1.0 is full size
     * 0.5 is half size and so on
     */
    fun calculateSize() : Double {
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

        // Numbers are not final, just to test
        if(importantNumber <= 7) {
            currentGridSize = 1.0
            return 1.0
        }
        else if(importantNumber <= 9) {
            currentGridSize = 0.8
            return 0.8
        }
        else {
            currentGridSize = 0.6
            return 0.6
        }
    }

    /**
     * Returns the fitting visual for a tile
     */
    fun tileVisual(tile: PrisonerTile) : ImageVisual {
        return ImageVisual("tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
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

        rootService.currentGame?.players?.get(rootService.currentGame!!.currentPlayer)?.apply {
            this.board.setPrisonGrid(2,2,true)
            this.board.setPrisonYard(2,2,PrisonerTile(13,PrisonerTrait.MALE,PrisonerType.RED))
        }
        gameScene.refreshAfterStartGame()
        showGameScene(gameScene)
    }
}

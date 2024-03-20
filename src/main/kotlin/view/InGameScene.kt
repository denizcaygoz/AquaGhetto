package view

import entity.Player
import entity.PrisonBus
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.RootService
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.gamecomponentviews.TokenView
import tools.aqua.bgw.event.KeyCode
import tools.aqua.bgw.components.layoutviews.CameraPane
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.*
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.CompoundVisual
import tools.aqua.bgw.visual.Visual
import java.awt.Color
import java.util.*
import kotlin.math.absoluteValue

class InGameScene(var rootService: RootService, test: SceneTest = SceneTest()) : BoardGameScene(1920,1080), Refreshable {

    // Assets on screen
    private val prisons: MutableList<PlayerBoard> = mutableListOf()
    private val prisonBuses: MutableList<BoardPrisonBus> = mutableListOf()
    private val isolations: MutableList<BoardIsolation> = mutableListOf()
    private val names: MutableList<Label> = mutableListOf()
    private val drawStack = TokenView(
        posX = 845,
        posY = 505,
        height = 70,
        width = 70,
        visual = ImageVisual("tiles/default_drawStack.png")
    ).apply {
        isDraggable = true
    }
    private val finalStack = TokenView(
        posX = 785,
        posY = 515,
        height = 50,
        width = 50,
        visual = ImageVisual("tiles/default_finalStack.png")
    ).apply {
        isDisabled = true
    }

    // Camera Pane stuff
    private val targetLayout = Pane<ComponentView>(width = 1920, height = 1080)
    private val cameraPane =
        CameraPane(width = 1920, height = 1080, target = targetLayout, visual = ColorVisual.DARK_GRAY).apply {
            isHorizontalLocked = false
            isVerticalLocked = false
            isZoomLocked = false
        }
    var statGui = Pane<ComponentView>(posX = 1790, width = 130, height = 1080, visual = ColorVisual.LIGHT_GRAY)


    init {
        rootService.addRefreshables(
            this
        )

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


        // Add the cameraPane to the scene
        addComponents(
            cameraPane,
            statGui
        )
    }

    /**
     * Transforms coordinates from the service layer
     * to view coordinates
     */
    fun coordsToView(serviceX: Int, serviceY: Int): Pair<Int, Int> {
        return Pair(serviceX + 10, -serviceY + 10)
    }

    /**
     * Transforms coordinates from the view layer
     * to service layer coordinates
     */
    fun coordsToService(viewX: Int, viewY: Int): Pair<Int, Int> {
        return Pair(viewX - 10, -viewY + 10)
    }

    fun replacePrison(player : Player) {
        for (i in 0 until prisons.size) {
            if (player.name == prisons[i].player.name) {
                val tempX = prisons[i].posX
                val tempY = prisons[i].posY
                prisons[i] = PlayerBoard(player, rootService)
                prisons[i].apply{
                    posX = tempX
                    posY = tempY
                }
            }
        }
    }

    fun tileVisual(tile: PrisonerTile): ImageVisual {
        return ImageVisual("tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
    }

    fun getPlayerBoard(player : Player) : PlayerBoard? {
        for (i in 0..prisons.size) {
            if (player == prisons[i].player) {
                return prisons[i]
            }
        }
        return null
    }

    override fun refreshAfterStartGame() {
        val game = rootService.currentGame
        checkNotNull(game) { "There is no game running" }
        val playerCount = game.players.size

        // Prison Grids and Isolations
        for (i in 0 until playerCount) {
            prisons.add(PlayerBoard(game.players[i], rootService))
            isolations.add(BoardIsolation(game.players[i].isolation))
            names.add(Label(text = game.players[i].name, font = Font(size = 20, color = Color.WHITE)))
        }
        // Prison Busses in Middle
        for (i in 0 until game.prisonBuses.size) {
            prisonBuses.add(BoardPrisonBus(game.prisonBuses[i]))
            prisonBuses[i].posX = (1000 + i * 60).toDouble()
            prisonBuses[i].posY = 540.0
        }
        if (playerCount == 5) {
            prisons[0].apply {
                posX = 960.0
                posY = 850.0
            }
            prisons[1].apply {
                posX = 380.0
                posY = 600.0
            }
            prisons[2].apply {
                posX = 680.0
                posY = 230.0
            }
            prisons[3].apply {
                posX = 1240.0
                posY = 230.0
            }
            prisons[4].apply {
                posX = 1540.0
                posY = 600.0
            }
        }
        for (i in 0 until playerCount) {
            isolations[i].posX = prisons[i].posX
            isolations[i].posY = prisons[i].posY + 180

            names[i].posX = prisons[i].posX
            names[i].posY = prisons[i].posY - 180
        }

        /*
        PLAYER POSITIONING FOR ALL PLAYER COUNTS TO BE ADDED
         */

        targetLayout.addAll(prisons)
        targetLayout.addAll(prisonBuses)
        targetLayout.addAll(isolations)
        targetLayout.addAll(names)
        targetLayout.addAll(drawStack, finalStack)


    }

    override fun refreshPrison(tile: PrisonerTile?, x: Int, y: Int) {
        val game = rootService.currentGame
        checkNotNull(game) { "There is no game running" }
        val player = game.currentPlayer

        if (tile == null) {
            prisons[player][coordsToView(x, y).first, coordsToView(x, y).second] =
                TokenView(
                    height = 50 * prisons[player].currentGridSize, width = 50 * prisons[player].currentGridSize,
                    visual = ImageVisual("tiles/default_tile.png")
                )
        } else {
            prisons[player][coordsToView(x, y).first, coordsToView(x, y).second] =
                TokenView(
                    height = 50 * prisons[player].currentGridSize, width = 50 * prisons[player].currentGridSize,
                    visual = prisons[player].tileVisual(tile)
                )
        }


    }

    override fun refreshAfterNextTurn(player: Player) {

        val ownGui = Pane<ComponentView>(width = 130, height = 1080, visual = ColorVisual.LIGHT_GRAY)

        val bigExtension =
            TokenView(
                posY = 10,
                posX = 10,
                height = 100,
                width = 100,
                visual = ImageVisual("tiles/big_expansion_tile.png")
            ).apply {
                isDraggable = true
                name = "big_extension"
                isDisabled = false
            }

        val smallExtension =
            TokenView(
                posY = 120,
                posX = 10,
                height = 100,
                width = 100,
                visual = ImageVisual("tiles/small_expansion_tile.png")
            ).apply {
                isDraggable = true
                name = "small_extension"
                isDisabled = false
            }


        ownGui.addAll(bigExtension, smallExtension)

        onKeyPressed = { event ->
            if (event.keyCode == KeyCode.R) {
                print("r was typed")
                val game = rootService.currentGame
                if (!smallExtension.isDisabled && game != null) {
                    when (prisons[game.currentPlayer].smallExtensionRotation) {
                        0 -> {
                            prisons[game.currentPlayer].smallExtensionRotation = 90
                            smallExtension.apply { rotation = 90.0 }
                        }

                        90 -> {
                            prisons[game.currentPlayer].smallExtensionRotation = 180
                            smallExtension.apply { rotation = 180.0 }
                        }

                        180 -> {
                            prisons[game.currentPlayer].smallExtensionRotation = 270
                            smallExtension.apply { rotation = 270.0 }
                        }

                        270 -> {
                            prisons[game.currentPlayer].smallExtensionRotation = 0
                            smallExtension.apply { rotation = 0.0 }
                        }
                    }
                }
            }
        }
        // Extension toggles Slots
        bigExtension.onMousePressed = {
            prisons[rootService.currentGame!!.currentPlayer].toggleExpansionSlots()
        }
        bigExtension.onMouseReleased = {
            prisons[rootService.currentGame!!.currentPlayer].toggleExpansionSlots()
        }
        smallExtension.onMousePressed = {
            prisons[rootService.currentGame!!.currentPlayer].toggleExpansionSlots()
        }
        smallExtension.onMouseReleased = {
            prisons[rootService.currentGame!!.currentPlayer].toggleExpansionSlots()
        }

        addComponents(ownGui)
    }


    inner class PlayerBoard(val player: Player, val rootService: RootService) :
        GridPane<TokenView>(rows = 21, columns = 21, layoutFromCenter = true) {

        // Both get patched by calculateSize()
        var currentGridSize = calculateSize()

        var smallExtensionRotation = 0

        init {
            this.spacing = 1.0 * currentGridSize

            // Iterator for grid
            val gridIterator = player.board.getPrisonGridIterator()
            while (gridIterator.hasNext()) {
                val entry = gridIterator.next()
                val x = entry.key
                val yMap = entry.value

                for ((y, floor) in yMap) {
                    if (floor) {
                        val tempX = coordsToView(x, y).first
                        val tempY = coordsToView(x, y).second
                        this[tempX, tempY] = TokenView(
                            height = 50 * currentGridSize,
                            width = 50 * currentGridSize,
                            visual = ImageVisual("tiles/default_tile.png")
                        )
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
                    val slot = this[coordsToView(x, y).first, coordsToView(x, y).second]
                    checkNotNull(slot) { "There is no Yard at the given coordinates x:$x and y: $y (View cords)" }
                    slot.apply {
                        this.visual = tileVisual(player.board.getPrisonYard(x, y) as PrisonerTile)
                    }
                }
            }
            var tempX = coordsToView(0, 0).first
            var tempY = coordsToView(0, 0).second
            this[tempX, tempY] = TokenView(
                height = 50 * currentGridSize,
                width = 50 * currentGridSize,
                visual = ImageVisual("tiles/no_tile.png")
            ).apply {
                this.isDisabled = true
            }
            tempX = coordsToView(1, 0).first
            tempY = coordsToView(1, 0).second
            this[tempX, tempY] = TokenView(
                height = 50 * currentGridSize,
                width = 50 * currentGridSize,
                visual = ImageVisual("tiles/no_tile.png")
            ).apply {
                this.isDisabled = true
            }
            tempX = coordsToView(0, 1).first
            tempY = coordsToView(0, 1).second
            this[tempX, tempY] = TokenView(
                height = 50 * currentGridSize,
                width = 50 * currentGridSize,
                visual = ImageVisual("tiles/no_tile.png")
            ).apply {
                this.isDisabled = true
            }
        }

        // Assisting methods from here on

        /**
         * Returns the size, which the prison has to have on screen.
         * 1.0 is full size
         * 0.5 is half size and so on
         *
         * And adjusts the expansion slots
         */
        fun changeSize() {
            for(x in 0.. 20) {
                for(y in 0.. 20) {
                    if(this[x,y] != null) {
                        if(this[x,y]!!.height == 50.0) {
                            this[x,y].apply{
                                height = 20.0
                                width = 20.0
                            }
                        }
                    }
                }
            }
        }

        fun calculateSize(): Double {
            // Save the span of the grid first
            var maxX = -10
            var minX = 10
            var maxY = -10
            var minY = 10
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
                if ((maxX.absoluteValue - minX.absoluteValue) >= (maxY.absoluteValue - minY.absoluteValue)) {
                    (maxX.absoluteValue - minX.absoluteValue)
                } else (maxY.absoluteValue - minY.absoluteValue)

            // Numbers are not final, just to test
            if (importantNumber <= 4) {
                currentGridSize = 1.0
            } else if (importantNumber <= 6) {
                currentGridSize = 0.8
            } else {
                currentGridSize = 0.6
            }

            // Calculating the border of the new Tile placements
            // minX, maxX, minY, maxY

            minX = if (minX - 2 >= -10) {
                minX - 2
            } else -10
            maxX = if (maxX + 2 <= 10) {
                maxX + 2
            } else 10
            minY = if (minY - 2 >= -10) {
                minY - 2
            } else -10
            maxY = if (maxY + 2 <= 10) {
                maxY + 2
            } else 10

            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    if (!player.board.getPrisonGrid(x, y)) {
                        this[coordsToView(x, y).first, coordsToView(x, y).second] =
                            TokenView(
                                height = 50 * currentGridSize,
                                width = 50 * currentGridSize,
                                visual = ImageVisual("tiles/expansion_tile.png")
                            ).apply {
                                this.isVisible = false
                                this.name = "expansion"
                                this.dropAcceptor = { dragEvent ->
                                    when (dragEvent.draggedComponent.name) {
                                        "big_extension" -> true
                                        "small_extension" -> true
                                        else -> {
                                            false
                                        }
                                    }
                                }

                                this.onDragDropped = { dragEvent ->
                                    when (dragEvent.draggedComponent.name) {
                                        "big_extension" -> {
                                            rootService.playerActionService.expandPrisonGrid(true, x, y, 0)
                                            //calculateSize()
                                        }


                                        "small_extension" -> {
                                            rootService.playerActionService.expandPrisonGrid( false, x, y, smallExtensionRotation)
                                            //calculateSize()
                                        }
                                    }
                                }
                                this.onDragGestureEnded
                            }
                    }
                }
            }

            return currentGridSize
        }

        fun toggleExpansionSlots() {
            for (x in 0..20) {
                for (y in 0..20) {
                    val currentButton = this[x, y]
                    if (currentButton != null)
                        if (currentButton.name == "expansion") {
                            currentButton.apply {
                                if (this.isVisible) this.isVisible = false
                                else this.isVisible = true
                            }
                        }
                }

            }
        }


        /**
         * Returns the fitting visual for a prison tile
         */
        fun tileVisual(tile: PrisonerTile): ImageVisual {
            return ImageVisual("tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
        }

    }


    inner class BoardPrisonBus(val bus: PrisonBus) : GridPane<TokenView>(rows = 3, columns = 1, layoutFromCenter = true) {

        /**
         * Fills the bus with tiles
         */
        init {
            this.spacing = 1.0
            for (i in 0 until bus.tiles.size) {
                if (bus.tiles[i] == null) {
                    this[0, i] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_tile.png"))
                } else if (bus.tiles[i] is GuardTile) {
                    this[0, i] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_guard.png"))
                } else this[0, i] =
                    TokenView(height = 50, width = 50, visual = tileVisual(bus.tiles[i] as PrisonerTile))
            }
        }

    }

    inner class BoardIsolation(val isolation: Stack<PrisonerTile>) :
        GridPane<TokenView>(rows = 1, columns = 120, layoutFromCenter = true) {

        init {
            this.spacing = 0.5
            if (isolation.isNotEmpty()) {
                for (i in 0 until isolation.size) {
                    if (i == 0) {
                        this[i, 0] = TokenView(height = 40, width = 40, visual = tileVisual(isolation[i]))
                    } else {
                        this[i, 0] = TokenView(height = 25, width = 25, visual = tileVisual(isolation[i])).apply {
                            isDisabled = true
                        }
                    }
                }
            }
        }

        fun tileVisual(tile: PrisonerTile): ImageVisual {
            return ImageVisual("tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
        }
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

        rootService.currentGame?.players?.get(0)?.apply {
            this.coins = 6
            this.board.setPrisonGrid(2,2,true)
            this.board.setPrisonYard(2,2,PrisonerTile(13,PrisonerTrait.MALE,PrisonerType.RED))
            for(i in 0..4) {
                this.isolation.add(PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED))
            }
        }
        rootService.currentGame?.players?.get(1)?.apply {
            this.coins = 6 }
        rootService.currentGame?.players?.get(2)?.apply {
            this.coins = 5 }
        rootService.currentGame?.players?.get(3)?.apply {
            this.coins = 5 }
        rootService.currentGame?.players?.get(4)?.apply {
            this.coins = 5 }
        showGameScene(gameScene)
    }
}

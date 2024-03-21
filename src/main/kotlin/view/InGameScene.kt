package view

import entity.Player
import entity.PrisonBus
import entity.enums.PlayerType
import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import entity.tileTypes.CoinTile
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import entity.tileTypes.Tile
import service.RootService
import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.components.gamecomponentviews.TokenView
import tools.aqua.bgw.components.layoutviews.CameraPane
import tools.aqua.bgw.components.layoutviews.GridPane
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.event.DropEvent
import tools.aqua.bgw.event.KeyCode
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.visual.ImageVisual
import java.awt.Color
import java.util.*
import kotlin.math.absoluteValue

/**
 * Returns the fitting visual for a prison tile
 */
fun tileVisual(tile: Tile?) : ImageVisual {
    val imgPath = when (tile) {
        is PrisonerTile -> "tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png"
        is GuardTile -> "tiles/default_guard.png"
        else -> "tiles/default_tile.png" // richtige CoinTile visuals?
    }

    return ImageVisual(imgPath)
}

class InGameScene(var rootService: RootService) : BoardGameScene(1920,1080), Refreshable {

    private var tileDrawn = false
    private var bonusToPlace = 0
    private var bonusTiles = mutableListOf<PrisonerTile>() /*do not edit*/

    // Quick Access
    private val defaultVisual = ImageVisual("tiles/default_tile.png")
    private val guardVisual = ImageVisual("tiles/default_guard.png")

    // Assets on screen
    private val prisons: MutableList<PlayerBoard> = mutableListOf()
    private var prisonBuses: MutableList<BoardPrisonBus> = mutableListOf()
    private val isolations: MutableList<BoardIsolation> = mutableListOf()
    private val names: MutableList<Label> = mutableListOf()
    private var ownGui = Pane<ComponentView>(width = 130, height = 1080, visual = ColorVisual.LIGHT_GRAY)
    private var statGui = Pane<ComponentView>(posX = 1790, width = 130, height = 1080, visual = ColorVisual.LIGHT_GRAY)
    private var currentPlayerLabel = Label(posX = 1780, posY = 50, height = 30, font = Font(color = Color.WHITE), text = "TESTTEST")
    private val freePrisonerButton : Button = Button(1640,950,150,50,text = "Free Prisoner!"
    ).apply {
        onMouseClicked = {
            val game = rootService.currentGame
            requireNotNull(game) {"no game"}
            val player = game.players[game.currentPlayer]
            if (player.isolation.isNotEmpty() && player.coins >= 2) { /*button does nothing if action is not valid*/
                rootService.playerActionService.freePrisoner()
            }
        }
    }
    private var drawnServiceTile : Tile? = null
    private val drawnTile = TokenView(posX = 785, posY = 465, height = 150, width = 150, visual = ImageVisual("tiles/default_tile.png")
    ).apply {
        isDisabled = true; isVisible = false;
    }
    private val drawStack = TokenView(posX = 855, posY = 500, height = 80, width = 80, visual = ImageVisual("tiles/default_drawStack.png")
    ).apply {
        onMouseClicked = {
            if(!tileDrawn) {
                tileDrawn = true
                drawnTile.isDisabled = false
                drawnTile.isVisible = true
                ownGui.isDisabled = true
                for (i in hideLabels) {
                    i.apply {
                        i.isDisabled = false
                        i.isVisible = true
                    }
                }

                if(rootService.currentGame!!.drawStack.isNotEmpty())
                {
                    drawnServiceTile = rootService.currentGame!!.drawStack.pop()
                    if (drawnServiceTile is CoinTile) {
                        drawnTile.visual = ImageVisual("tiles/default_coin.png")
                    }
                    if (drawnServiceTile is GuardTile) {
                        drawnTile.visual = ImageVisual("tiles/default_guard.png")
                    }
                    if (drawnServiceTile is PrisonerTile) {
                        drawnTile.visual = tileVisual(drawnServiceTile as PrisonerTile)
                    }
                }
            }
        }
    }
    private val finalStack = TokenView(posX = 785, posY = 515, height = 50, width = 50, visual = ImageVisual("tiles/default_finalStack.png")
    ).apply {
        isDisabled = true
    }
    // Stuff to hide other stuff
    val hideLabels : List<Label> = listOf(
        Label(height=460, width = 1920, visual = ColorVisual.BLACK).apply {
            opacity = 0.5; isVisible = false
        } ,
        Label(posY = 620, height=460, width = 1920, visual = ColorVisual.BLACK).apply {
            opacity = 0.5; isVisible = false
        },
        Label(posY = 460, height=160, width = 710, visual = ColorVisual.BLACK).apply {
            opacity = 0.5; isVisible = false
        },
        Label(posY = 460, posX = 1310 ,height=160, width = 1920, visual = ColorVisual.BLACK).apply {
            opacity = 0.5; isVisible = false
        })

    // Camera Pane stuff

    private val targetLayout = Pane<ComponentView>(width = 1920, height = 1080)
    private val cameraPane =
        CameraPane(width = 1920, height = 1080, target = targetLayout, visual = ColorVisual.DARK_GRAY).apply {
            isHorizontalLocked = false
            isVerticalLocked = false
            isZoomLocked = false
        }

    init {
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
            freePrisonerButton,
            statGui,
            hideLabels[0], hideLabels[1], hideLabels[2], hideLabels[3],
            currentPlayerLabel,
        )
    }

    fun coordsToView(serviceX: Int, serviceY: Int): Pair<Int, Int> {
        return Pair(serviceX + 10, -serviceY + 10)
    }

    fun coordsToService(viewX: Int, viewY: Int): Pair<Int, Int> {
        return Pair(viewX - 10, -viewY + 10)
    }

    fun placePrisoner(target : TokenView, visual : ImageVisual) : ImageVisual {
        val oldTarget : ImageVisual = target.visual as ImageVisual
        target.visual = visual
        return oldTarget
    }

    fun tileVisual(tile: PrisonerTile): ImageVisual {
        try {
            return ImageVisual("tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
        }
        catch (e : Exception) {
            print("Missing Tile: tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
        }
        return ImageVisual("tiles/no_tile.png")
    }

    fun getPlayerBoard(player : Player) : PlayerBoard? {
        for (i in 0 until prisons.size) {
            if (player.name == prisons[i].player.name) {
                return prisons[i]
            }
        }
        return null
    }

    override fun refreshPrisonBus(prisonBus: PrisonBus?) {
        /*just refresh all prison buses*/


        val game = rootService.currentGame
        requireNotNull(game)

        targetLayout.removeAll(prisonBuses)

        prisonBuses = mutableListOf()
        if (rootService.currentGame!!.prisonBuses.size == 0) return
        for (i in 0 until game.prisonBuses.size) {
            prisonBuses.add(BoardPrisonBus(game.prisonBuses[i]))
            prisonBuses[i].posX = (1000 + i * 60).toDouble()
            prisonBuses[i].posY = 540.0
            prisonBuses[i].apply {
                name = ""
                for (j in 0 until this.bus.tiles.size) {
                    if (bus.blockedSlots[j]) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/no_tile.png")).apply { name = ""} } else
                    if (bus.tiles[j] == null) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_tile.png")).apply { name = ""} } else
                    if (bus.tiles[j] is CoinTile) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_coin.png")).apply { name = ""} } else
                    if (bus.tiles[j] is GuardTile) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_guard.png")).apply { name = ""} } else
                    if (bus.tiles[j] is PrisonerTile) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = tileVisual(bus.tiles[j] as PrisonerTile)).apply { name = ""} }

                    this.name = "bus_${j}_board"
                    this[0,j]!!.name = "busTile_${j}_false"
                }
            }
        }

        /*
        if (prisonBus == null) {

        }
        else {
            for(i in 0 until game.prisonBuses.size) {
                if(prisonBuses[i].bus == prisonBus) {
                    prisonBuses[i].apply {
                        name = ""
                        for (j in 0 until this.bus.tiles.size) {
                            if (bus.tiles[j] == null) {
                                this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_tile.png")).apply { name = ""; isDraggable = false}}
                            if (bus.tiles[j] is CoinTile) {
                                this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_coin.png")).apply { name = ""; isDraggable = false}}
                            if (bus.tiles[j] is GuardTile) {
                                this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_guard.png")).apply { name = ""; isDraggable = false}}
                            if (bus.tiles[j] is PrisonerTile) {
                                this[0, j] = TokenView(height = 50, width = 50, visual = tileVisual(bus.tiles[j] as PrisonerTile)).apply { name = ""; isDraggable = false}}

                            this.name = "bus_${j}_board"
                            this[0,j]!!.name = "busTile_${j}_false"
                        }
                    }
                }
            }
        }
        */

        for(i in 0 until game.players.size) {
            val takenBus = game.players[i].takenBus
            if(takenBus != null) {

                val bus = BoardPrisonBus(takenBus)
                prisonBuses.add(bus)
                bus.posX = getPlayerBoard(rootService.currentGame!!.players[i])!!.posX - 200
                bus.posY = getPlayerBoard(rootService.currentGame!!.players[i])!!.posY
                bus.name = "bus_${i}_true"

                for (j in 0 until bus.bus.tiles.size) {
                    if (bus.bus.tiles[j] == null) {
                        bus[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_tile.png")).apply { name = ""; isDraggable = false}}
                    if (bus.bus.tiles[j] is CoinTile) {
                        bus[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_coin.png")).apply { name = ""; isDraggable = false}}
                    if (bus.bus.tiles[j] is GuardTile) {
                        bus[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_guard.png")).apply { name = ""; isDraggable = false}}
                    if (bus.bus.tiles[j] is PrisonerTile) {
                        bus[0, j] = TokenView(height = 50, width = 50, visual = tileVisual(bus.bus.tiles[j] as PrisonerTile)).apply { name = ""; isDraggable = false}}

                    bus[0,j]!!.name = "busTile_${j}_false"
                }

                bus.apply {
                    name = ""
                    for(i in 0 until rootService.currentGame!!.players.size) {
                        val player = rootService.currentGame!!.players[i]
                        if(player.takenBus != null) {
                            for(j in 0 until prisonBuses.size) {
                                if (player.takenBus == prisonBuses[j].bus) {
                                    prisonBuses[j].apply {
                                        posX = getPlayerBoard(player)!!.posX - 200
                                        posY = getPlayerBoard(player)!!.posY
                                        this.name = "bus_${i}_true}"
                                        for (k in 0 until this.bus.tiles.size) {
                                            if (this.bus.tiles[k] != null) {
                                                this[0, k]!!.name = "busTile_${i}_${this.bus.tiles[k]!!.id}_true}"
                                                val tempBoardPrisonBus = this
                                                val tempPrisonerTile = this.bus.tiles[k]!!
                                                this[0, k]!!.apply {
                                                    isDraggable = true
                                                    onDragGestureEnded = { event, success ->
                                                        println("success: $success")
                                                        busDoGestureEndStuff(
                                                            event,
                                                            tempPrisonerTile,
                                                            tempBoardPrisonBus
                                                        )
                                                        player.takenBus!!.tiles[k] = null
                                                        var elementsExist = false
                                                        for (l in 0 until player.takenBus!!.tiles.size) {
                                                            if (player.takenBus!!.tiles[l] != null) {
                                                                elementsExist = true
                                                            }
                                                        }
                                                        if (!elementsExist) rootService.gameService.determineNextPlayer(
                                                            true
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                /*
                for(j in 0 until prisonBuses.size) {
                    if (rootService.currentGame!!.players[i].takenBus == prisonBuses[j].bus) {
                        prisonBuses[j].apply {
                            posX = getPlayerBoard(rootService.currentGame!!.players[i])!!.posX - 200
                            posY = getPlayerBoard(rootService.currentGame!!.players[i])!!.posY
                            this.name = "bus_${i}_true"
                            for (k in 0 until this.bus.tiles.size) {
                                if (this.bus.tiles[k] != null) {
                                    this[0, k]!!.name = "busTile_${i}_${this.bus.tiles[k]!!.id}_true}"
                                    val temp2 = this
                                    val temp = this.bus.tiles[k]!!
                                    this[0, k]!!.apply {
                                        isDraggable = true
                                        onDragGestureEnded = {event, success ->
                                            println("success: $success")
                                            busDoGestureEndStuff(event,temp, temp2)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                */


            }
        }

        targetLayout.addAll(prisonBuses)

    }

    fun busDoGestureEndStuff(dropEvent: DropEvent,tile : Tile, prisonBus: BoardPrisonBus) {
        val targetList = dropEvent.dragTargets

        if(prisonBus.bus.tiles.isEmpty()) {
            rootService.gameService.determineNextPlayer(true)
            return
        }
        for (element in targetList) {
            val name = element.name
            if (!name.contains("dropTile_")) continue
            val splitInfo = name.split("_")
            val playerIndexLocation = Integer.parseInt(splitInfo[1])
            val locXVisual = Integer.parseInt(splitInfo[2])
            val locYVisual = Integer.parseInt(splitInfo[3])

            val loc = coordsToService(locXVisual, locYVisual)

            println("place tile info $playerIndexLocation    ${loc.first}    ${loc.second}")

            val game = rootService.currentGame
            requireNotNull(game)
            val currentPlayerIndex = game.currentPlayer

            prisonBus.isTurnEnded()
            if (currentPlayerIndex == playerIndexLocation) {
                /*player moving on own grid*/
                if (!rootService.validationService.validateTilePlacement(tile as PrisonerTile, loc.first, loc.second)) {
                    println("invalid placemente")
                    /*no valid location, refreshIsolation*/
                    refreshPrisonBus(null)
                    prisonBus.isTurnEnded()
                    return
                }
                val bonus = rootService.playerActionService.placePrisoner(tile as PrisonerTile,loc.first,loc.second)
                refreshPrison(tile as PrisonerTile, loc.first, loc.second)
                for (i in prisonBus.bus.tiles.indices) {
                    val tileABC = prisonBus.bus.tiles[i] ?: continue
                    if (tileABC.id  == tile.id) {
                        prisonBus.bus.tiles[i] = null
                    }
                }
                refreshPrisonBus(null)
                doBonusStuff(currentPlayerIndex, bonus, busTaken = true, game.players[currentPlayerIndex].takenBus!!.tiles.isEmpty())
            } else {
                /*player tries to place prisoner on other grid, this is not allowed*/
                refreshPrisonBus(null)
                prisonBus.isTurnEnded()
                return
            }

            println("Element: " + element.name)

            break
        }
    }

    override fun refreshTileStack(finalStack: Boolean) {
        if (finalStack) {
            this.finalStack.isVisible = false
            this.finalStack.isDisabled= true
            this.drawStack.visual = this.finalStack.visual
        }
    }

    override fun refreshScoreStats() {
        statGui = Pane<ComponentView>(posX = 1790, width = 130, height = 1080, visual = ColorVisual.LIGHT_GRAY)

        val playerLabels : MutableList<Label> = mutableListOf()
        for(i in 0 until prisons.size) {
            playerLabels.add(Label(posY = i*200, height = 400, font = Font(color = Color.WHITE)).apply {
                text =  "${prisons[i].player.name}:\n\n" +
                        "Score: ${prisons[i].player.currentScore} \n" +
                        "Coins: ${prisons[i].player.coins} \n" +
                        "Has Janitor: \n ${prisons[i].player.hasJanitor} \n" +
                        "Secretary Count: \n ${prisons[i].player.secretaryCount} \n" +
                        "Lawyer Count: \n ${prisons[i].player.lawyerCount}"
            } )
        }
        removeComponents(statGui)
        statGui.addAll(playerLabels)
        addComponents(statGui)
    }

    override fun refreshAfterStartGame() {
        val game = rootService.currentGame
        checkNotNull(game) { "There is no game running" }
        val playerCount = game.players.size

        /*remove old values*/
        targetLayout.removeAll(prisons)
        targetLayout.removeAll(isolations)
        targetLayout.removeAll(names)
        targetLayout.removeAll(prisonBuses)
        prisons.clear()
        isolations.clear()
        names.clear()
        prisonBuses.clear()

        // Prison Grids and Isolations
        for (i in 0 until playerCount) {
            prisons.add(PlayerBoard(game.players[i], rootService))
            isolations.add(BoardIsolation(game.players[i].isolation, i))
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
        if (playerCount == 4) {
            prisons[0].apply {
                posX = 1240.0
                posY = 850.0
            }
            prisons[1].apply {
                posX = 680.0
                posY = 850.0
            }
            prisons[2].apply {
                posX = 680.0
                posY = 230.0
            }
            prisons[3].apply {
                posX = 1240.0
                posY = 230.0
            }
        }
        if (playerCount == 3) {
            prisons[0].apply {
                posX = 960.0
                posY = 850.0
            }
            prisons[1].apply {
                posX = 680.0
                posY = 230.0
            }
            prisons[2].apply {
                posX = 1240.0
                posY = 230.0
            }
        }
        if (playerCount == 2) {
            prisons[0].apply {
                posX = 380.0
                posY = 600.0
            }
            prisons[1].apply {
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
        targetLayout.addAll(drawStack, finalStack, drawnTile)

        refreshScoreStats()
    }

    override fun refreshPrison(tile: Tile?, x: Int, y: Int) {
        val game = rootService.currentGame
        checkNotNull(game) { "There is no game running" }
        val player = game.currentPlayer
        prisons[player].player = game.players[player]
        /*currentPlayerlabel */
        val playerName = game.players[game.currentPlayer].name
        removeComponents(currentPlayerLabel)
        currentPlayerLabel.text = "Current Player:\n${playerName}"
        addComponents(currentPlayerLabel)
        /*currentPlayerlabel */

        val tileGuard = game.players[player].board.getPrisonYard(x,y)
        if (tileGuard is GuardTile) {
            prisons[player][coordsToView(x, y).first, coordsToView(x, y).second] =
                TokenView(
                    height = 50 * prisons[player].currentGridSize, width = 50 * prisons[player].currentGridSize,
                    visual = ImageVisual("tiles/default_guard.png")
                )
            return
        }

        if (tile == null) {
            prisons[player][coordsToView(x, y).first, coordsToView(x, y).second] =
                TokenView(
                    height = 50 * prisons[player].currentGridSize, width = 50 * prisons[player].currentGridSize,
                    visual = ImageVisual("tiles/default_tile.png")
                ).apply {name = "takenBusTileTarget"} //TODO add drop for isolation tiles here
        } else if (tile is PrisonerTile) {
            prisons[player][coordsToView(x, y).first, coordsToView(x, y).second] =
                TokenView(
                    height = 50 * prisons[player].currentGridSize, width = 50 * prisons[player].currentGridSize,
                    visual = prisons[player].tileVisual(tile)
                )
        }
    }

    override fun refreshAfterNextTurn(player: Player) {
        ownGui = Pane<ComponentView>(width = 130, height = 1080, visual = ColorVisual.LIGHT_GRAY)

        val game = rootService.currentGame
        requireNotNull(game)
        /*update copied*/
        for (i in prisons.indices) {
            prisons[i].player = game.players[i]
        }

        removeComponents(currentPlayerLabel)
        currentPlayerLabel.text = "Current Player:\n${player.name}"
        addComponents(currentPlayerLabel)

        val bigExtension =
            TokenView(posY = 10, posX = 10, height = 100, width = 100, visual = ImageVisual("tiles/big_expansion_tile.png")
            ).apply {
                isDraggable = true
                name = "big_extension"
                isDisabled = false
            }

        val smallExtension =
            TokenView( posY = 120, posX = 10, height = 100, width = 100, visual = ImageVisual("tiles/small_expansion_tile.png")
            ).apply {
                isDraggable = true
                name = "small_extension"
                isDisabled = false
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

                    }
                }
            }

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


        /*var tileIterator = getPlayerBoard(player)!!.iterator()
        for(x in 0..20)
            for(y in 0..20) {
            }
         */

        removeComponents(ownGui)
        ownGui.addAll(bigExtension, smallExtension)
        addComponents(ownGui)
    }

    //TODO
    override fun refreshIsolation(player: Player) {
        println("refreshIsolation called.")
        val game = rootService.currentGame
        requireNotNull(game) {"game null"}
        var indexPlayer = -1
        for (i in game.players.indices) {
            if (game.players[i].name == player.name) indexPlayer = i
        }

        val isolation = isolations[indexPlayer]
        println("isolation to refresh $indexPlayer")

        isolation.refreshIsolation()
    }

    override fun refreshGuards(
        player: Player,
        sourceCoords: Pair<Int, Int>?,
        destCoords: Pair<Int, Int>?
    ) {
        val game = rootService.currentGame!!
        val playerIndex = game.players.indexOf(player)
        val playerBoard = prisons[playerIndex]

        sourceCoords?.let {
            val (sourceX, sourceY) = it
            val (gridX, gridY) = coordsToView(sourceX, sourceY)
            var playerBoardTile = playerBoard[gridX, gridY]
        }

        destCoords?.let {
            val (destX, destY) = it
            val (gridX, gridY) = coordsToView(destX, destY)
            val playerBoardTile = playerBoard[gridX, gridY]
            playerBoardTile?.visual = guardVisual
            playerBoardTile?.isDraggable = true
            playerBoardTile?.name = "guard_${playerIndex}_${destX}_${destY}"
        }
    }

    inner class PlayerBoard(var player: Player, val rootService: RootService) :
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
                        val (tempX, tempY) = coordsToView(x, y)
                        this[tempX, tempY] = TokenView(
                            height = 50 * currentGridSize,
                            width = 50 * currentGridSize,
                            visual = ImageVisual("tiles/default_tile.png")
                        ).apply {
                            val game = rootService.currentGame
                            requireNotNull(game)
                            name = "dropTile_${game.players.indexOf(player)}_${tempX}_${tempY}"

                            //TODO
                            /*important merge this loop with other drag events on the prison*/
                            //coordsToView(x, y)
                            dropAcceptor = {
                                var result = false
                                val fromIsolation = it.draggedComponent.name.contains("isolation_")
                                val fromTakenBus = it.draggedComponent.name.contains("busTile") && it.draggedComponent.name.contains("true")
                                val fromGuardMove = it.draggedComponent.name.contains("guard_")
                                println("fromIsolation: $fromIsolation")
                                println("fromGuardMove: $fromGuardMove")
                                if (fromIsolation) {
                                    val textSplit = it.draggedComponent.name.split("_")
                                    val playerIsolation = Integer.parseInt(textSplit[1])
                                    val card = textSplit[2]
                                    val currentPlayer = game.currentPlayer
                                    result = if (currentPlayer == playerIsolation) {
                                        /*from own isolation*/
                                        /*player needs one coin*/
                                        game.players[game.currentPlayer].coins >= 1
                                    } else {
                                        /*from other isolation*/
                                        /*player needs 2 coins*/
                                        game.players[game.currentPlayer].coins >= 2
                                    }
                                } else if (it.draggedComponent.name.contains("newGuard_")) {
                                    /*drop was new Guard*/
                                    val textSplit = it.draggedComponent.name.split("_")
                                    val playerIndex = Integer.parseInt(textSplit[1])
                                    if (playerIndex != game.currentPlayer) {
                                        result = false /*moved to other grid*/
                                    } else {
                                        result = (player.board.getPrisonGrid(x,y) && player.board.getPrisonYard(x,y) == null)
                                    }
                                } else if (it.draggedComponent.name.contains("newPrisoner_")) {
                                    println("prisoner")
                                    //name = "newPrisoner_${playerIndex}_${tile.id}"
                                    val textSplit = it.draggedComponent.name.split("_")
                                    val playerIndex = Integer.parseInt(textSplit[1])
                                    val indexCard = Integer.parseInt(textSplit[2])
                                    println("$playerIndex ${game.currentPlayer} $indexCard")
                                    if (playerIndex != game.currentPlayer) {
                                        result = false /*moved to other grid*/
                                    } else {
                                        var tile: PrisonerTile? = null
                                        for (t in bonusTiles) {
                                            if (t.id == indexCard) {
                                                tile = t
                                                break
                                            }
                                        }
                                        if (tile == null) {
                                            for (t in game.allTiles) {
                                                if (t.id == indexCard && t is PrisonerTile) {
                                                    tile = t
                                                    break
                                                }
                                            }
                                        }
                                        if (tile == null) throw IllegalStateException("found no bonus card to check")
                                        result = rootService.validationService.validateTilePlacement(tile, x, y)
                                    }
                                } else if (fromGuardMove) {
                                    result = game.players[game.currentPlayer].coins >= 1
                                }
                                if(fromTakenBus) {
                                    val textSplit = it.draggedComponent.name.split("_")
                                    result = true
                                }


                                result
                            }

                            onDragDropped = {
                                val tileName = it.draggedComponent.name
                                when {
                                    tileName.contains("guard_") -> {
                                        val currentGame = rootService.currentGame
                                        requireNotNull(currentGame)
                                        val currentPlayer = currentGame.players[currentGame.currentPlayer]

                                        tileName.split("_")
                                            .takeLast(2)
                                            .let { coords ->
                                                val (sourceX, sourceY) = coords.map { pos -> pos.toInt() }
                                                println("Moving Guard for \"${currentPlayer.name}\": ($sourceX, $sourceY) -> ($x, $y)")
                                                check(currentPlayer.coins >= 1) {
                                                    "${currentPlayer.name} has only ${currentPlayer.coins} coins"
                                                }
                                                rootService.playerActionService.moveEmployee(
                                                    sourceX, sourceY, x, y
                                                )
                                                println(currentPlayer.coins)
                                            }
                                    }
                                }
                            }




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
                    val slot = this[coordsToView(x, y).first, coordsToView(x, y).second]
                    checkNotNull(slot) { "There is no Yard at the given coordinates x:$x and y: $y (View cords)" }
                    slot.apply {
                        this.visual = tileVisual(player.board.getPrisonYard(x, y))
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
                for (j in 0 until this.bus.tiles.size) {
                    if (bus.tiles[j] == null) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_tile.png"))
                    }
                    if (bus.tiles[j] is CoinTile) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_coin.png"))
                    }
                    if (bus.tiles[j] is GuardTile) {
                        this[0, j] = TokenView(height = 50, width = 50, visual = ImageVisual("tiles/default_guard.png"))
                    }
                    if (bus.tiles[j] is PrisonerTile) {
                        this[0, j] =
                            TokenView(height = 50, width = 50, visual = tileVisual(bus.tiles[j] as PrisonerTile))
                    }

                    this.name = "bus_${j}_board"
                    this[0, j]!!.name = "busTile_${j}_false"
                }
                this.apply {
                    this.onMouseClicked = {
                        if (tileDrawn) {
                            if (drawnServiceTile != null) {
                                rootService.playerActionService.addTileToPrisonBus(drawnServiceTile!!, bus)
                            }
                            tileDrawn = false
                            drawnTile.isDisabled = true
                            drawnTile.isVisible = false
                            ownGui.isDisabled = false
                            for (j in hideLabels) {
                                j.apply {
                                    j.isDisabled = true
                                    j.isVisible = false
                                }
                            }
                        } else if (this.name.contains("bus_") && this.name.contains("board")) {
                            rootService.playerActionService.takePrisonBus(this.bus)
                        }
                    }
                }
            }
        }

        fun isTurnEnded() {
            if (bus.tiles.isEmpty()) rootService.gameService.determineNextPlayer(true)
        }

    }

    inner class BoardIsolation(var isolation: Stack<PrisonerTile>, val playerIndex: Int) :
        GridPane<TokenView>(rows = 1, columns = 120, layoutFromCenter = true) {

        init {
            this.refreshIsolation()
        }

        fun doGestureEndStuff(dropEvent: DropEvent, nameDropped: String) {
            val targetList = dropEvent.dragTargets
            for (element in targetList) {
                val name = element.name
                if (!name.contains("dropTile_")) continue
                val splitInfo = name.split("_")
                val playerIndexLocation = Integer.parseInt(splitInfo[1])
                val locXVisual = Integer.parseInt(splitInfo[2])
                val locYVisual = Integer.parseInt(splitInfo[3])

                val loc = coordsToService(locXVisual, locYVisual)

                println("place tile info $playerIndexLocation    ${loc.first}    ${loc.second}")

                val game = rootService.currentGame
                requireNotNull(game)
                val currentPlayerIndex = game.currentPlayer

                if (currentPlayerIndex == playerIndexLocation) {
                    /*player moving on own grid*/
                    if (!rootService.validationService.validateTilePlacement(isolation.peek(), loc.first, loc.second)) {
                        println("invalid placemente")
                        /*no valid location, refreshIsolation*/
                        refreshIsolation()
                        return
                    }

                    val nameSplit = nameDropped.split("_")
                    val sourcePlacerIndex = Integer.parseInt(nameSplit[1])

                    val currentPlayer = game.players[currentPlayerIndex]
                    val sourcePlayer = game.players[sourcePlacerIndex]
                    if (sourcePlacerIndex == playerIndexLocation) {
                        /*from own isolation*/
                        if (currentPlayer.coins < 1) {
                            refreshIsolation()
                            return
                        }
                        val bonus = rootService.playerActionService.movePrisonerToPrisonYard(loc.first, loc.second)
                        doBonusStuff(currentPlayerIndex, bonus, busTaken = false, true)
                    } else {
                        /*from other isolation*/
                        if (currentPlayer.coins < 2) {
                            refreshIsolation()
                            return
                        }
                        val bonus = rootService.playerActionService.buyPrisonerFromOtherIsolation(sourcePlayer, loc.first, loc.second)
                        doBonusStuff(currentPlayerIndex, bonus, busTaken = false, true)
                    }

                } else {
                    /*player tries to place prisoner on other grid, this is not allowed*/
                    refreshIsolation()
                    return
                }

                println("Element: " + element.name)
                break
            }
        }

        fun refreshIsolation() {
            val game = rootService.currentGame
            requireNotNull(game) {"game is null"}
            val player = game.players[playerIndex]
            this.isolation = player.isolation

            this.spacing = 0.5
            for (i in 0 until columns) {
                this[i,0] = null
            }
            if (isolation.isNotEmpty()) {
                for (i in 0 until isolation.size) {
                    if (i == 0) {
                        this[i, 0] = TokenView(height = 40, width = 40, visual = tileVisual(isolation[isolation.size - i - 1])).apply {
                            val tile = isolation[i]
                            isDraggable = true
                            name = "isolation_${playerIndex}_${tile.id}"
                            isDisabled = false
                            onDragGestureEnded = { event, success ->
                                println("success: $success")
                                doGestureEndStuff(event, name)
                            }
                        }
                    } else {
                        this[i, 0] = TokenView(height = 25, width = 25, visual = tileVisual(isolation[isolation.size - i - 1])).apply {
                            isDisabled = true
                        }
                    }
                }
            } else {
                this[0, 0] = TokenView(height = 40, width = 40, visual = ImageVisual("tiles/no_tile.png")).apply {
                    isDisabled = true
                }
            }
        }

        fun tileVisual(tile: PrisonerTile): ImageVisual {
            return ImageVisual("tiles/${tile.prisonerType}_${tile.prisonerTrait}_tile.png")
        }
    }

    /*creates new Token view and forces the player to place it*/
    private fun doBonusStuff(playerIndex: Int, bonus: Pair<Boolean,PrisonerTile?>, busTaken: Boolean, nextPlayer: Boolean) {

        if (bonus.first) {
            handleBonusWorker(playerIndex)
            bonusToPlace++
        }
        val baby = bonus.second
        if (baby != null) {
            handleBonusBaby(playerIndex, baby)
            bonusToPlace++
        }

        if (nextPlayer) rootService.gameService.determineNextPlayer(busTaken)
    }

    fun handleBonusWorker(playerIndex: Int) {
        val guardTile = TokenView(200, 850,height = 50, width = 50, visual = ImageVisual("tiles/default_guard.png")).apply {
            isDraggable = true
            name = "newGuard_${playerIndex}_guard"
            isDisabled = false
            onDragGestureEnded = { event, success ->
                println("success: $success")
                placeGuard(event , this)
            }
        }
        addComponents(guardTile)
    }

    fun handleBonusBaby(playerIndex: Int, tile: PrisonerTile) {
        bonusTiles.add(tile)
        val baby = TokenView(200, 850, height = 50, width = 50, visual = tileVisual(tile)).apply {
            isDraggable = true
            name = "newPrisoner_${playerIndex}_${tile.id}"
            isDisabled = false
            onDragGestureEnded = { event, success ->
                println("success: $success")
                placeTileStuff(event, tile, playerIndex, this)
            }
        }
        addComponents(baby)
    }

    private fun placeTileStuff(dropEvent: DropEvent, tile: PrisonerTile, playerIndex: Int, tokenView: TokenView) {
        val targetList = dropEvent.dragTargets
        for (element in targetList) {
            val name = element.name
            if (!name.contains("dropTile_")) continue
            val splitInfo = name.split("_")
            val playerIndexLocation = Integer.parseInt(splitInfo[1])
            val locXVisual = Integer.parseInt(splitInfo[2])
            val locYVisual = Integer.parseInt(splitInfo[3])

            val loc = coordsToService(locXVisual, locYVisual)

            println("place tile info $playerIndexLocation    ${loc.first}    ${loc.second}")

            val game = rootService.currentGame
            requireNotNull(game)
            val currentPlayerIndex = game.currentPlayer

            val bonus = rootService.playerActionService.placePrisoner(tile, loc.first, loc.second)
            bonusToPlace--
            this.doBonusStuff(playerIndex, bonus, false, false)
            bonusTiles.remove(tile)
            removeComponents(tokenView)
            break
        }
    }

    private fun placeGuard(dropEvent: DropEvent, tokenView: TokenView) {
        val targetList = dropEvent.dragTargets
        for (element in targetList) {
            val name = element.name
            if (name.contains("dropTile_")) {
                val splitInfo = name.split("_")
                val playerIndexLocation = Integer.parseInt(splitInfo[1])
                val locXVisual = Integer.parseInt(splitInfo[2])
                val locYVisual = Integer.parseInt(splitInfo[3])

                val loc = coordsToService(locXVisual, locYVisual)

                println("place tile info $playerIndexLocation    ${loc.first}    ${loc.second}")

                val game = rootService.currentGame
                requireNotNull(game)
                rootService.playerActionService.moveEmployee(-101,-101,loc.first,loc.second)
                bonusToPlace--
                removeComponents(tokenView)
                break
            } else if (name.contains("abc")) { //TODO other locations

            }

        }
    }

}
/*
/*
/
 * Below this are methods for testing the IngameScene
 */
fun main() {
    val test = SceneTest()
    test.show()

}

class SceneTest : BoardGameApplication("AquaGhetto"), Refreshable {
    private val rootService = RootService()
    private val gameScene = InGameScene(rootService)

    init {
        rootService.gameService.startNewGame(mutableListOf(
            Pair("Moin0", PlayerType.PLAYER),
            Pair("Moin1", PlayerType.PLAYER)))/*
            Pair("Moin2", PlayerType.PLAYER),
            Pair("Moin3", PlayerType.PLAYER),
            Pair("Moin4", PlayerType.PLAYER))*/

        /*
        rootService.currentGame?.players?.get(0)?.apply {
            this.coins = 10
            this.board.setPrisonGrid(2,2,true)
            //this.board.setPrisonYard(2,2,PrisonerTile(13,PrisonerTrait.MALE,PrisonerType.RED))
            for(i in 0..4) {
                //this.isolation.add(PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.RED))
            }
        }

        rootService.playerActionService.placePrisoner(PrisonerTile(13, PrisonerTrait.MALE, PrisonerType.GREEN), -100,-100)
        rootService.playerActionService.placePrisoner(PrisonerTile(14, PrisonerTrait.FEMALE, PrisonerType.GREEN), -100,-100)
        rootService.playerActionService.placePrisoner(PrisonerTile(14, PrisonerTrait.FEMALE, PrisonerType.GREEN), -100,-100)
        rootService.playerActionService.placePrisoner(PrisonerTile(14, PrisonerTrait.FEMALE, PrisonerType.GREEN), -100,-100)
        rootService.playerActionService.placePrisoner(PrisonerTile(14, PrisonerTrait.FEMALE, PrisonerType.GREEN), -100,-100)
        rootService.playerActionService.placePrisoner(PrisonerTile(14, PrisonerTrait.MALE, PrisonerType.GREEN), -100,-100)

        rootService.currentGame?.players?.get(1)?.apply {
            this.coins = 6 }
        rootService.currentGame?.players?.get(2)?.apply {
            this.coins = 5 }
        rootService.currentGame?.players?.get(3)?.apply {
            this.coins = 5 }
        rootService.currentGame?.players?.get(4)?.apply {
            this.coins = 5 }
        rootService.playerActionService.moveEmployee(-101, -101, 2, 3)
        showGameScene(gameScene)*/
    }
}
*/
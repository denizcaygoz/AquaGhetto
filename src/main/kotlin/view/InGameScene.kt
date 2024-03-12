package view

import service.RootService
import tools.aqua.bgw.core.BoardGameApplication
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.visual.ImageVisual
import entity.enums.PlayerType

class InGameScene(rootService : RootService) : BoardGameScene(5940,3240) , Refreshable {

    init {
        background = ImageVisual("Test_BackGround_Ingame.png")
    }
}


/**
 * Below this are methods for testing the IngameScene
 */
fun main() {
    val test = SceneTest()
}

class SceneTest() : BoardGameApplication("AquaGhetto"), Refreshable  {
    private val rootService = RootService()
    private val gameScene = InGameScene(rootService)

    init {
        rootService.gameService.startNewGame(mutableListOf(Pair("Moin",PlayerType.PLAYER),Pair("Moin2",PlayerType.PLAYER)))
        showGameScene(gameScene)
    }
}

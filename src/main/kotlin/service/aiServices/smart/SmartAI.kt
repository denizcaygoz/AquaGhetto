package service.aiServices.smart

import entity.AquaGhetto
import entity.Player
import entity.aIActions.*
import entity.enums.PlayerType
import entity.tileTypes.GuardTile
import entity.tileTypes.PrisonerTile
import service.RootService
import service.aiServices.smart.evaluateActions.*

class SmartAI(val rootService: RootService, val player: Player) {

    private val evaluateActionFreePrisoner = EvaluateFreePrisonerService(this)
    private val evaluateActionTakeBus = EvaluateTakeBusService(this)
    private val evaluateAddTileToBus = EvaluateAddTileToPrisonBusService(this)
    private val evaluateBuyPrisoner = EvaluateBuyPrisonerService(this)
    private val evaluateExpandPrison = EvaluateExpandPrisonGridService(this)
    private val evaluateMoveEmployee = EvaluateMoveEmployeeService(this)
    private val evaluateMoveOwnPrisoner = EvaluateMoveOwnPrisonerService(this)

    init {
        require(player.type == PlayerType.AI) {"Player is not an AI"} /*der spieler der die züge machen soll*/
    }

    /*wird von ai service aufgerufen*/
    /*ruft dann intern minMax auf und so*/
    /*mal schauen ob wir hier multithreading einbauen, mehr als ein rechner zur berechnung?*/
    fun makeTurn(game: AquaGhetto) {
        val action = this.minMax(game, 10, true, 0)
        this.executeAction(game, action)
    }

    private fun executeAction(game: AquaGhetto, aiAction: AIAction) {
        when (aiAction) {
            is ActionAddTileToBus -> {
                val bus = game.prisonBuses[aiAction.indexBus]
                val tile = GuardTile() //TODO replace with proper function drawCard
                rootService.playerActionService.addTileToPrisonBus(tile, bus)
            }
            is ActionMovePrisoner -> {
                val placeCard = aiAction.placeCard
                val prisoner = placeCard.placePrisoner
                val bonus = rootService.playerActionService.movePrisonerToPrisonYard(prisoner.first, prisoner.second)
                this.placeCardBonus(aiAction.placeCard, bonus)
            }
            is ActionMoveEmployee -> {
                val source = aiAction.source
                val destination = aiAction.destination
                rootService.playerActionService.moveEmployee(source.first, source.second,
                    destination.first, destination.first)
            }
            is ActionBuyPrisoner -> {
                val boughtFrom = aiAction.buyFrom
                val placeCard = aiAction.placeCard
                val prisoner = placeCard.placePrisoner
                val bonus = rootService.playerActionService.buyPrisonerFromOtherIsolation(boughtFrom,
                    prisoner.first, prisoner.second)
                this.placeCardBonus(aiAction.placeCard, bonus)
            }
            is ActionFreePrisoner -> {
                rootService.playerActionService.freePrisoner()
            }
            is ActionExpandPrison -> {
                val isBig = aiAction.isBig
                val location = aiAction.location
                val rotation = aiAction.rotation
                rootService.playerActionService.expandPrisonGrid(isBig, location.first, location.second, rotation)
            }
            is ActionTakeBus -> {
                takeBus(aiAction, game)
            }
            else -> {
                println("End no action")
            }
        }
    }

    private fun takeBus(aiAction: ActionTakeBus, game: AquaGhetto) {
        val takenBus = game.prisonBuses[aiAction.bus]
        rootService.playerActionService.takePrisonBus(takenBus) /*remove coins from bus*/
        val tiles = takenBus.tiles.filterNotNull()
        if (aiAction.placeCards.size != tiles.size) {
            //TODO add emergency action?
            println("Error AI action did not matched bus tiles")
            return
        }
        for (i in tiles.indices) {
            val tile = tiles[i]
            if (tile !is PrisonerTile) {
                println("Found non prisoner tile in bus, this should not happen")
                continue
            }
            val placeCard = aiAction.placeCards[i]
            val prisoner = placeCard.placePrisoner
            val bonus = rootService.playerActionService.placePrisoner(tile, prisoner.first, prisoner.second)
            this.placeCardBonus(placeCard, bonus)
        }
    }

    /*
     * actionsInspected ist nur dafür da um zu verfolgen wie viele wege schon bewertet wurden
     * damit kann einfacher gesagt werden welche optimierungen weniger wege betrachten
     * kann später sonst auch gelöscht werden
     */
    fun minMax(game: AquaGhetto, depth: Int, maximize: Boolean, actionsChecked: Int): AIAction {
        if (depth == 0 || checkGameEnd()) { /*hier überprüfung ob maximale tiefe erreicht wurde oder spiel schon geendet hat*/
            return AIAction(false, evaluateCurrentPosition())
        }

        /*die verschiedenen züge die ein spieler machen kann, wenn ein zug nicht möglich ist, hat dieser den schlechtesten wert
        * abhängig von maximize + oder - unendlich*/
        /*funktion gibt den score des standes nach der aktion zurück und was gemacht werden muss damit man zu diesem
        * score kommt, damit dies nicht ernuet berechnet werden muss*/
        /*erstelle objekte sollten sich (hoffentlich) in grenzen halten*/
        val scoreAddTileToPrisonBus = evaluateAddTileToBus.getScoreAddTileToPrisonBus(game, depth, maximize, actionsChecked)
        val scoreMoveOwnPrisoner = evaluateMoveOwnPrisoner.getScoreMoveOwnPrisoner(game, depth, maximize, actionsChecked)
        val scoreMoveEmployee = evaluateMoveEmployee.getScoreMoveEmployee(game, depth, maximize, actionsChecked)
        val scoreBuyPrisoner = evaluateBuyPrisoner.getScoreBuyPrisoner(game, depth, maximize, actionsChecked)
        val scoreFreePrisoner = evaluateActionFreePrisoner.freePrisoner(game, depth, maximize, actionsChecked)
        val scoreExpandPrison = evaluateExpandPrison.getScoreExpandPrisonGrid(game, depth, maximize, actionsChecked)
        val scoreTakeBus = evaluateActionTakeBus.takeBus(game, depth, maximize, actionsChecked)

        val scoreList = mutableListOf(scoreAddTileToPrisonBus, scoreMoveOwnPrisoner, scoreMoveEmployee,
            scoreBuyPrisoner, scoreFreePrisoner, scoreExpandPrison, scoreTakeBus)

        val bestAction = this.getBestAction(maximize, scoreList)

        return bestAction
    }

    private fun getBestAction(maximize: Boolean, actionList: List<AIAction>): AIAction {
        return if (maximize) {
            actionList.maxBy { action: AIAction ->  action.score}
        } else {
            actionList.minBy { action: AIAction ->  action.score}
        }
    }

    fun checkGameEnd(): Boolean {
        return false /*game end wenn alle spieler fertig und finalStack.size != 15*/
    }

    fun evaluateCurrentPosition(): Int {
        return 0 /*hier ist ne gute evaluation funktion sehr wichtig*/
    }

    /**
     * Function to place a card bonus depending on the info provided in [placeCard] and [bonus]
     *
     * @param placeCard info about the location, where to place a tile
     * @param bonus the bonus obtained by placing a card
     */
    private fun placeCardBonus(placeCard: PlaceCard , bonus: Pair<Boolean,PrisonerTile?>) {
        /*place possible employee if valid*/
        placeEmployee(bonus.first, placeCard.firstTileBonusEmployee)

        /*place possible baby*/
        var secondBonus: Pair<Boolean, PrisonerTile?>? = null
        val bonusBaby = bonus.second
        val bonusLocation = placeCard.placeBonusPrisoner
        if (bonusBaby != null && bonusLocation == null) {
            //TODO add emergency action?
            println("Error AI action did not matched bonus")
        } else if (bonusBaby == null && bonusLocation != null) {
            /*do nothing I guess*/
            println("Error AI action did not matched bonus")
        } else if (bonusBaby != null && bonusLocation != null) {
            secondBonus = rootService.playerActionService.placePrisoner(bonusBaby,
                bonusLocation.first, bonusLocation.second)
        }

        /*Second bonus can only "contain" a new employee*/
        if (secondBonus == null) return
        placeEmployee(secondBonus.first, placeCard.secondTileBonusEmployee)
    }

    /**
     * Places a prisoner at the location if provided and if bonus is true
     */
    private fun placeEmployee(bonus: Boolean, employee: Pair<Int,Int>?) {
        if (bonus && (employee == null)) {
            //TODO add emergency action?
            println("Error AI action did not matched bonus")
        } else if (!bonus && (employee != null)) {
            /*do nothing I guess*/
            println("Error AI action did not matched bonus")
        } else if (bonus && employee != null) {
            rootService.playerActionService.moveEmployee(-101,-101,
                employee.first, employee.second)
        }
    }

}
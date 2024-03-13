package service.aiServices.smart

import entity.AquaGhetto
import entity.Player
import entity.aIActions.*
import entity.enums.PlayerType
import service.aiServices.smart.evaluateActions.*

class SmartAI(val player: Player) {

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

    fun executeAction(game: AquaGhetto, aiAction: AIAction) {
        when (aiAction) {
            is ActionAddTileToBus -> {
                /*do action in different function*/
            }
            is ActionMovePrisoner -> {
                /*do action in different function*/
            }
            is ActionMoveEmployee -> {
                /*do action in different function*/
            }
            is ActionBuyPrisoner -> {
                /*do action in different function*/
            }
            is ActionFreePrisoner -> {
                /*do action in different function*/
            }
            is ActionExpandPrison -> {
                /*do action in different function*/
            }
            is ActionTakeBus -> {
                /*do action in different function*/
            }
            else -> {
                println("End no action")
            }
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

}
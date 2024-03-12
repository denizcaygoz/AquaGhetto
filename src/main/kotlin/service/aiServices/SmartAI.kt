package service.aiServices

import entity.AquaGhetto
import entity.Player
import entity.aIActions.*
import entity.enums.PlayerType

class SmartAI(val player: Player) {

    init {
        require(player.type == PlayerType.AI) {"Player is not an AI"} /*der spieler der die züge machen soll*/
    }

    /*wird von ai service aufgerufen*/
    /*ruft dann intern minMax auf und so*/
    /*mal schauen ob wir hier multithreading einbauen, mehr als ein rechner zur berechnung?*/
    fun makeTurn(game: AquaGhetto) {
        val action = this.minMax(game, 10, true, 0)
    }

    fun executeAction(ame: AquaGhetto, aiAction: AIAction) {
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
    fun minMax(game: AquaGhetto, depth: Int, maximize: Boolean, actionsInspected: Int): AIAction {
        if (depth == 0 || checkGameEnd()) { /*hier überprüfung ob maximale tiefe erreicht wurde oder spiel schon geendet hat*/
            return AIAction(evaluateCurrentPosition())
        }

        /*die verschiedenen züge die ein spieler machen kann, wenn ein zug nicht möglich ist, hat dieser den schlechtesten wert
        * abhängig von maximize + oder - unendlich*/
        /*funktion gibt den score des standes nach der aktion zurück und was gemacht werden muss damit man zu diesem
        * score kommt, damit dies nicht ernuet berechnet werden muss*/
        /*erstelle objekte sollten sich (hoffentlich) in grenzen halten*/
        val scoreAddTileToPrisonBus = this.getScoreAddTileToPrisonBus(game, depth, maximize, actionsInspected)
        val scoreMoveOwnPrisoner = this.getScoreMoveOwnPrisoner(game, depth, maximize, actionsInspected)
        val scoreMoveEmployee = this.getScoreMoveEmployee(game, depth, maximize, actionsInspected)
        val scoreBuyPrisoner = this.getScoreBuyPrisoner(game, depth, maximize, actionsInspected)
        val scoreFreePrisoner = this.freePrisoner(game, depth, maximize, actionsInspected)
        val scoreExpandPrison = this.getScoreExpandPrisonGrid(game, depth, maximize, actionsInspected)
        val scoreTakeBus = this.takeBus(game, depth, maximize, actionsInspected)

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

    fun takeBus(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionTakeBus {
        return ActionTakeBus(0, PlaceCard(Pair(0,0)))
        /*erneuter aufruf von minmax um den score der nächsten aktion zu bekommen, hier nur score relevant*/
        /*gilt für alle methoden die ab hier kommen*/
        /*am besten kann man das hier auch auslagern später in eigende Klassen*/
    }

    fun freePrisoner(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionFreePrisoner {
        return ActionFreePrisoner(0)
    }

    fun getScoreBuyPrisoner(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionBuyPrisoner {
        return ActionBuyPrisoner(0, player, PlaceCard(Pair(0,0)))
    }

    fun getScoreMoveEmployee(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionMoveEmployee {
        return ActionMoveEmployee(0, Pair(0,0), Pair(0,0))
    }

    fun getScoreAddTileToPrisonBus(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionAddTileToBus {
        return ActionAddTileToBus(0, 0)
    }

    fun getScoreMoveOwnPrisoner(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionMovePrisoner {
        return ActionMovePrisoner(0, PlaceCard(Pair(0,0)))
    }

    fun getScoreExpandPrisonGrid(game: AquaGhetto, depth: Int, maximize: Boolean, amountActions: Int): ActionExpandPrison {
        return ActionExpandPrison(0, Pair(0,0) , 0)
    }

}
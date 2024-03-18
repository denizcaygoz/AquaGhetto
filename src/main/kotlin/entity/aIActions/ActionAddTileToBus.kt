package entity.aIActions

import entity.Player
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile

class ActionAddTileToBus(validAction: Boolean,
                         score: Int,
                         val indexBusCoin: Int,
                         val indexBusPrisoner: MutableMap<PrisonerType, Int>): AIAction(validAction, score)
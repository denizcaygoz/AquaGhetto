package entity.aIActions

import entity.Player
import entity.enums.PrisonerType
import entity.tileTypes.PrisonerTile

/**
 * Represents an AI action of adding a tile to a bus.
 *
 * @property validAction Indicates whether the action is valid.
 * @property score The score associated with this action.
 * @property indexBusCoin The index of the bus where a coin is added.
 * @property indexBusPrisoner A map containing the index of the bus for each prisoner type to be added.
 * @constructor Initializes an ActionAddTileToBus object with the specified parameters.
 */
class ActionAddTileToBus(validAction: Boolean,
                         score: Int,
                         val indexBusCoin: Int,
                         val indexBusPrisoner: MutableMap<PrisonerType, Int>): AIAction(validAction, score)
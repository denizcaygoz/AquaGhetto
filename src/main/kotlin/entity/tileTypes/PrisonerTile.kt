package entity.tileTypes

import entity.enums.PrisonerTrait
import entity.enums.PrisonerType

class PrisonerTile(val prisonerTrait: PrisonerTrait , val prisonerType: PrisonerType): Tile() {

    val breedable: Int = 0

}
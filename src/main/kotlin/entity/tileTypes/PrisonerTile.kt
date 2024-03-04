package entity.tileTypes

import entity.enums.PrisonerTrait
import entity.enums.PrisonerType

/**
 * Data class to represent a prisoner tile
 * @see [Tile]
 *
 * @param prisonerTrait the trait of this prisoner
 * @param prisonerType the type of this prisoner
 * @property breedable indicates if a prisoner is breedable, is true if prisoner has the
 * trait male or female and has not yet been paired.
 */
class PrisonerTile(val prisonerTrait: PrisonerTrait , val prisonerType: PrisonerType): Tile() {

    var breedable: Boolean = (prisonerTrait == PrisonerTrait.MALE) || (prisonerTrait == PrisonerTrait.FEMALE)

}
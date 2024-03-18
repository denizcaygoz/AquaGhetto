package entity.tileTypes

import entity.enums.PrisonerTrait
import entity.enums.PrisonerType
import java.io.Serializable

/**
 * Data class to represent a prisoner tile
 * @see [Tile]
 *
 * @param id the unique identifier of this card
 * @param prisonerTrait the trait of this prisoner
 * @param prisonerType the type of this prisoner
 * @property breedable indicates if a prisoner is breedable, is true if prisoner has the
 * trait male or female and has not yet been paired.
 */
class PrisonerTile(override val id: Int,
                   val prisonerTrait: PrisonerTrait ,
                   val prisonerType: PrisonerType): Tile(), Serializable, Cloneable {
    companion object {
        private const val serialVersionUID: Long = -8184519218972531765L
    }

    var breedable: Boolean = (prisonerTrait == PrisonerTrait.MALE) || (prisonerTrait == PrisonerTrait.FEMALE)

    public override fun clone(): PrisonerTile {
        return PrisonerTile(id, prisonerTrait, prisonerType).apply { breedable = this@PrisonerTile.breedable }
    }
}
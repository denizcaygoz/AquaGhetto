package entity.aIActions

/**
 * Represents a place card.
 *
 * @property placePrisoner The coordinates for placing a prisoner tile.
 * @property placeBonusPrisoner The coordinates for placing a bonus prisoner tile.
 * @property firstTileBonusEmployee The coordinates for placing the first bonus employee tile.
 * @property secondTileBonusEmployee The coordinates for placing the second bonus employee tile.
 * @constructor Initializes a PlaceCard object with the specified parameters.
 */
class PlaceCard(val placePrisoner: Pair<Int,Int>,
                val placeBonusPrisoner: Pair<Int,Int>? = null,
                val firstTileBonusEmployee: Pair<Int,Int>? = null,
                val secondTileBonusEmployee: Pair<Int,Int>? = null)
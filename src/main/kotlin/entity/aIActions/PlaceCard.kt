package entity.aIActions

class PlaceCard(val placePrisoner: Pair<Int,Int>,
                val placeBonusPrisoner: Pair<Int,Int>? = null,
                val firstTileBonusEmployee: Pair<Int,Int>? = null,
                val secondTileBonusEmployee: Pair<Int,Int>? = null)
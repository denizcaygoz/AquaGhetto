package entity

import entity.tileTypes.Tile

class PrisonBus {

    val tiles: MutableList<Tile> = mutableListOf()
    val blockedSlots: MutableList<Boolean> = mutableListOf()

}
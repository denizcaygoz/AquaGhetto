package entity

import entity.tileTypes.Tile
import tools.aqua.bgw.util.Stack


/**
 * Entity class that represents a game state of "Aquaghetto".
 *
 * This class is used to store a list of players, the current player,
 * the normal draw pile, the final draw pile and the prison buses.
 *
 * @property drawStack the stack from which cards are drawn during the course of the game
 * @property finalStack the stack from which cards are drawn as soon as the [drawStack] is empty
 * As soon as a card is drawn from this stack, the current round is the last round
 * @property players a list of players participating in this game
 * The order of the players in the list is the order in which the players perform actions
 * @property currentPlayer the player whose turn it is
 * @property prisonBusses a list of [PrisonBus] which are located in the middle of the playing field
 * As soon as a player takes a prison bus, it is removed from the list
 */
class AquaGhetto {

    val drawStack: Stack<Tile> = Stack()
    val finalStack: Stack<Tile> = Stack()
    val players: MutableList<Player> = mutableListOf()
    var currentPlayer: Int = 0
    val prisonBusses: MutableList<PrisonBus> = mutableListOf()

}
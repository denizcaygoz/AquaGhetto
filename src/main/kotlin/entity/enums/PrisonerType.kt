package entity.enums

import java.io.Serializable

/**
 * Enum to distinguish between the eight possible prisoner types
 * red, blue, green, yellow, orange, purple, brown, cyan
 * (red, blue and green cards are not allowed to be removed)
 *
 * The corresponding animals from the game Aquaretto would be
 * dolphins, orcas, sea lions, hippopotamus, sea turtle, penguin, polar bear, crocodile
 * (dolphins, orcas, and sea lions cards are not allowed to be removed)
 */
enum class PrisonerType: Serializable {
    RED,
    BLUE,
    GREEN,
    YELLOW,
    ORANGE,
    PURPLE,
    BROWN,
    CYAN;
}
package entity.enums

/**
 * Enum to distinguish between the eight possible prisoner types
 * red, blue, green, yellow, orange, purple, brown, cyan
 * (red, blue and green cards are not allowed to be removed)
 *
 * The corresponding animals from the game Aquaretto would be
 * dolphins, orcas, sea lions, sea turtle, hippopotamus, crocodile, penguin, polar bear
 * (dolphins, orcas, and sea lions cards are not allowed to be removed)
 *
 * red -> dolphins
 * blue -> orcas
 * green -> sea lison
 * yellow -> sea turtle
 * orange -> hippopotamus
 * purple -> crocodile
 * brown -> penguin
 * cyan -> polar bear
 */
enum class PrisonerType {
    RED,
    BLUE,
    GREEN,
    YELLOW,
    ORANGE,
    PURPLE,
    BROWN,
    CYAN;
}
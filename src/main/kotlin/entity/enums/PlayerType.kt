package entity.enums

/**
 * Enum to distinguish between the three possible types a player can have
 * KI: a "smart" KI automatically doing actions
 * RANDOM: a "dumb" KI automatically doing random actions
 * PLAYER: a "normal" Player input is expected from the GUI
 * ONLINE: a "normal" Player input is expected from the Network
 */
enum class PlayerType {
    AI,
    RANDOM_AI,
    PLAYER,
    NETWORK,
}
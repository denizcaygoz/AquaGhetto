package entity.enums

import java.io.Serializable


/**
 * Enum to distinguish between the six possible traits a prisoner can have
 * none, male, female, old, armed, baby
 *
 * The corresponding traits/symbols from the game Aquaretto would be
 * none, male, female, flash, fish, offspring
 */
enum class PrisonerTrait: Serializable {
    NONE,
    MALE,
    FEMALE,
    OLD,
    RICH,
    BABY,
}
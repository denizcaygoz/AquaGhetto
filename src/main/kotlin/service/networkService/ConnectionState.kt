package service.networkService

/**
 * Enum to distinguish the different states that occur in networked games, in particular
 * during connection and game setup. Used in [NetworkService].
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTED,
    WAITING_FOR_HOST_CONFIRMATION,
    WAITING_FOR_JOIN_CONFIRMATION,
    WAITING_FOR_GUEST,
    WAITING_FOR_TURN,
    WAITING_FOR_INIT,
    PLAYING_MY_TURN
}
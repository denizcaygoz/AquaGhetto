package service

import entity.AquaGhetto
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Service layer class that provides basic functions to save and load the game, undo and redo game actions
 *
 * @param rootService instance of the [RootService] for access to other services
 */
class GameStatesService(private val rootService: RootService): AbstractRefreshingService() {

    /**
     * Saves the current game with all states of the game
     *
     * @throws IllegalStateException if there is no running game
     * @throws IllegalStateException if there was an error while saving the game
     */
    fun saveGame() {
        val game = rootService.currentGame
        checkNotNull(game) {"No game to save"}
        saveAquaGhetto(game)
    }

    /**
     * This function loads the saved game, sets the current game to the loaded one and updates the GUI.
     *
     * @return an instance of AquaGhetto saved in the file "saveFile"
     * @throws IllegalStateException if there was an error while loading the game
     */
    fun loadGame() {
        val loadedGame = loadAquaGhetto()
        rootService.currentGame = loadedGame
        onAllRefreshables {
            refreshAfterStartGame()
        }
    }

    /**
     * Undoes an action by loading the previous state of the game
     * Refreshes all relevant GUI elements
     * @throws IllegalStateException if there is no active game
     * @throws IllegalStateException if there is no previous state
     */
    fun undo() {
        val game = rootService.currentGame
        checkNotNull(game) {"No active game"}
        val oldState = game.previousState
        checkNotNull(oldState) {"Nothing to undo"}
        rootService.currentGame = oldState
        onAllRefreshables {
            refreshAfterStartGame()
        }
    }

    /**
     * Redoes an action by loading a newer state/ the next state of the game
     * Refreshes all relevant GUI elements
     *
     * @throws IllegalStateException if there is no active game
     * @throws IllegalStateException if there is no next state
     */
    fun redo() {
        val game = rootService.currentGame
        checkNotNull(game) {"No active game"}
        val nextState = game.nextState
        checkNotNull(nextState) {"Nothing to redo"}
        rootService.currentGame = nextState
        onAllRefreshables {
            refreshAfterStartGame()
        }
    }

    /**
     * Saves an instance of AquaGhetto to the file "saveFile"
     *
     * @param aquaGhetto the instance to save
     * @throws IOException see [FileOutputStream], [GZIPOutputStream], [ObjectOutputStream]
     * @throws SecurityException see [FileOutputStream], [ObjectOutputStream]
     * @throws NullPointerException see [ObjectOutputStream], [File]
     */
    private fun saveAquaGhetto(aquaGhetto: AquaGhetto) {
        val saveLocation = File("saveFile")
        val fileOut = FileOutputStream(saveLocation)
        val gzipOut = GZIPOutputStream(fileOut)
        val objOut = ObjectOutputStream(gzipOut)
        objOut.writeObject(aquaGhetto)
        objOut.close()
    }

    /**
     * Loads an instance of AquaGhetto from the file "saveFile"
     * This function only creates an instance of Aquaretto, but does not
     * set the current game to the loaded one and does not call a refresh.
     *
     * @return an instance of AquaGhetto saved in the file "saveFile"
     * @throws IOException see [FileInputStream], [GZIPInputStream], [ObjectInputStream]
     * @throws SecurityException see [FileInputStream], [ObjectInputStream]
     * @throws NullPointerException see [ObjectInputStream], [File]
     */
    private fun loadAquaGhetto(): AquaGhetto {
        val saveLocation = File("saveFile")
        check(saveLocation.exists()) {"There is no game to load"}
        val fileIn = FileInputStream(saveLocation)
        val gzipIn = GZIPInputStream(fileIn)
        val objIn = ObjectInputStream(gzipIn)
        val obj = objIn.readObject()
        check(obj is AquaGhetto) {"Loaded object was not an instance of AquaGhetto"}
        return obj
    }

    /**
     * Creates a deep copy of the current aquaghetto game.
     *
     * @return A copy of the current AquaGhetto game with the old instance kept as a reference
     * in [AquaGhetto.previousState].
     */
    fun copyAquaGhetto(): AquaGhetto {
        val currentGame = rootService.currentGame
        checkNotNull(currentGame) {"No active game"}
        val copiedGame = currentGame.clone()

        copiedGame.previousState = currentGame
        currentGame.nextState = copiedGame

        return copiedGame
    }

}
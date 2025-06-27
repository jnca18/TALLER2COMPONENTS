package com.example.taller2components.ViewModel
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taller2components.persistence.Player
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel responsable de gestionar la lógica de creación, unión y seguimiento de tableros de juego
 * para el juego 4 en línea, utilizando Firebase Firestore como backend en tiempo real.
 *
 * Esta clase permite:
 * - Crear un tablero nuevo.
 * - Unirse a un tablero existente.
 * - Escuchar en tiempo real los cambios de jugadores.
 * - Iniciar automáticamente el juego cuando hay dos jugadores.
 */
class GameBoardViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var playersListener: ListenerRegistration? = null

    // Exponemos el estado de los jugadores y errores
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun createGameBoard(): String {
        val boardId = db.collection("gameBoards").document().id
        val rows = 6
        val columns = 7
        val grillaVacia= generarGrillaComoLista(rows, columns)

        val gameBoard = mapOf(
            "id" to boardId,
            "rows" to rows,
            "columns" to columns,
            "players" to emptyList<Map<String, Any>>(), // jugadores se agregan luego
            "currentPlayerIndex" to 1,
            "gameStatus" to "waiting",
            "state" to false,
            "grid" to grillaVacia
        )

        db.collection("gameBoards").document(boardId)
            .set(gameBoard)
            .addOnSuccessListener {
                Log.d("createGameBoard", "Tablero creado correctamente con ID: $boardId")
            }
            .addOnFailureListener {
                Log.e("createGameBoard", "Error al crear el tablero", it)
            }

        return boardId
    }

    fun generarGrillaComoLista(rows: Int, columns: Int): List<Map<String, Any?>> {
        val grilla = mutableListOf<Map<String, Any?>>()
        for (fila in 0 until rows) {
            for (columna in 0 until columns) {
                val casilla = mapOf(
                    "fila" to fila,
                    "columna" to columna,
                    "valor" to 0,
                    "color" to null
                )
                grilla.add(casilla)
            }
        }
        return grilla
    }



    /**
     * Función para agregar un jugador al tablero
     */
    fun joinBoard(boardId: String, player: Player) {
        Log.d("JoinBoard", "Información de actualización: $boardId $player")
        val boardRef = db.collection("gameBoards").document(boardId)
        boardRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()

                    if (currentPlayers.any { it["correo"] == player.correo }) {
                        _errorMessage.value = "Ya estás en esta partida"
                        return@addOnSuccessListener
                    }

                    val playerData = mapOf(
                        "idPlayer" to player.idPlayer,
                        "correo" to player.correo,
                        "turno" to currentPlayers.size + 1,
                        "color" to player.color,
                        "isCurrentTurn" to false,
                        "score" to 0,
                        "wins" to 0,
                        "isHost" to currentPlayers.isEmpty()
                    )

                    val updatedPlayers = currentPlayers.toMutableList().apply {
                        add(playerData)
                    }

                    // Si hay 2 jugadores, se inicializa el juego
                    val updates = mutableMapOf<String, Any>(
                        "players" to updatedPlayers
                    )

                    if (updatedPlayers.size == 2) {
                        updates["state"] = true
                        updates["gameStatus"] = "playing"
                        updates["currentPlayerIndex"] = 1
                        val firstPlayer = updatedPlayers[0].toMutableMap()
                        firstPlayer["isCurrentTurn"] = true
                        updatedPlayers[0] = firstPlayer
                    }

                    boardRef.update(updates)
                        .addOnSuccessListener {
                            _errorMessage.value = null
                        }
                        .addOnFailureListener { exception ->
                            _errorMessage.value = exception.message
                        }
                } else {
                    _errorMessage.value = "No se encontró el tablero con ID: $boardId"
                }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al unirse al tablero: ${e.message}"
                Log.e("JoinBoard", "Error al unirse", e)
            }
    }

    /**
     * Limpia el listener cuando el ViewModel es destruido
     */
    override fun onCleared() {
        super.onCleared()
        playersListener?.remove()
    }
}


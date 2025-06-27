package com.example.taller2components.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taller2components.persistence.Board
import com.example.taller2components.persistence.Casilla
import com.example.taller2components.persistence.Player
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel encargado de la lógica principal del juego 4 en línea.
 *
 * Se encarga de:
 * - Escuchar los cambios del tablero en tiempo real desde Firebase Firestore.
 * - Exponer el estado del juego y jugadores mediante `StateFlow`.
 * - Manejar errores, estados de carga, cambios de turno y futuras operaciones como registrar movimientos.
 */
class GameViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var boardListener: ListenerRegistration? = null
    private var playersListener: ListenerRegistration? = null

    // Estado reactivo de los jugadores en el tablero
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    // Estado reactivo del tablero completo
    private val _board = MutableStateFlow<Board?>(null)
    val board: StateFlow<Board?> = _board

    // Mensaje de error actual (si lo hay)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Estado de carga de operaciones remotas
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Consulta un tablero desde Firestore en tiempo real.
     * Escucha continuamente los cambios y actualiza el estado local.
     *
     * @param idBoard ID del tablero a consultar.
     */
    fun consultarTablero(idBoard: String) {
        Log.d("consultarTablero", "Consultando tablero: $idBoard")
        _isLoading.value = true
        _errorMessage.value = null

        val boardRef = db.collection("gameBoards").document(idBoard)

        boardListener?.remove() // Evitar duplicados

        boardListener = boardRef.addSnapshotListener { snapshot, error ->
            _isLoading.value = false

            when {
                error != null -> {
                    _errorMessage.value = "Error al escuchar cambios: ${error.message}"
                    Log.e("consultarTablero", "Error en snapshot", error)
                    return@addSnapshotListener
                }

                snapshot == null || !snapshot.exists() -> {
                    _errorMessage.value = "No se encontró el tablero con ID: $idBoard"
                    return@addSnapshotListener
                }

                else -> {
                    try {
                        val data = snapshot.data!!
                        Log.d("consultarTablero", "Datos recibidos: ${data.keys}")

                        val board = try {
                            parseBoardData(data)
                        } catch (e: Exception) {
                            throw Exception("Error procesando datos del tablero: ${e.message}", e)
                        }

                        Log.d("consultarTablero", "Tablero actualizado: $board")
                        _board.value = board

                    } catch (e: Exception) {
                        _errorMessage.value = "Error al procesar datos: ${e.message}"
                        Log.e("consultarTablero", "Error de procesamiento", e)
                    }
                }
            }
        }
    }

    /**
     * Parsea los datos crudos de Firestore y reconstruye un objeto [Board].
     *
     * @param data Mapa con la información del tablero.
     * @return Objeto [Board] válido.
     */
    private fun parseBoardData(data: Map<String, Any>): Board {
        val id = data["id"] as? String ?: ""
        val rows = (data["rows"] as? Long)?.toInt() ?: 6
        val columns = (data["columns"] as? Long)?.toInt() ?: 7
        val gameStatus = data["gameStatus"] as? String ?: "waiting"
        val state = data["state"] as? Boolean ?: false
        val winner = data["winner"] as? String

        val playersRaw = data["players"] as? List<Map<String, Any>> ?: emptyList()
        val players = playersRaw.mapNotNull { playerData ->
            try {
                Player(
                    idPlayer = playerData["idPlayer"] as? String ?: "",
                    correo = playerData["correo"] as? String ?: "",
                    color = playerData["color"] as? String ?: "#000000",
                    score = (playerData["score"] as? Long)?.toInt() ?: 0,
                    isCurrentTurn = playerData["isCurrentTurn"] as? Boolean ?: false,
                    wins = (playerData["wins"] as? Long)?.toInt() ?: 0,
                    isHost = playerData["isHost"] as? Boolean ?: false,
                    isWinner = playerData["isWinner"] as? Boolean ?: false,
                    turno = (playerData["turno"] as? Long)?.toInt() ?: 1
                )
            } catch (e: Exception) {
                Log.e("parseBoardData", "Error parsing player ${playerData["idPlayer"]}", e)
                null
            }
        }

        val gridRaw = data["grid"] as? List<Map<String, Any?>> ?: emptyList()
        val gridMatrix = MutableList(rows) { MutableList(columns) { Casilla(0) } }

        for (item in gridRaw) {
            try {
                val fila = (item["fila"] as? Long)?.toInt() ?: continue
                val columna = (item["columna"] as? Long)?.toInt() ?: continue
                val valor = (item["valor"] as? Long)?.toInt() ?: 0
                val color = item["color"] as? String

                if (fila in 0 until rows && columna in 0 until columns) {
                    gridMatrix[fila][columna] = Casilla(fila, columna, valor, color.toString())
                }
            } catch (e: Exception) {
                Log.e("parseBoardData", "Error parsing grid cell: $item", e)
            }
        }

        return Board(
            id = id,
            rows = rows,
            columns = columns,
            players = players,
            state = state,
            currentPlayerIndex = 1,
            gameStatus = gameStatus,
            winner = winner,
            grid = gridMatrix
        )
    }

    /**
     * Escucha en tiempo real la lista de jugadores de un tablero.
     *
     * @param boardId ID del tablero a observar.
     */
    fun listenToPlayers(boardId: String) {
        val boardRef = db.collection("gameBoards").document(boardId)
        playersListener?.remove()

        playersListener = boardRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Error al escuchar cambios: ${error.message}"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    val players2 = snapshot.get("players") as? List<Map<String, Any>> ?: emptyList()
                    val playerList = players2.mapNotNull { data ->
                        try {
                            Log.d("currentUserId", data.toString())
                            Player(
                                idPlayer = data["idPlayer"] as? String ?: "",
                                correo = data["correo"] as? String ?: "",
                                color = data["color"] as? String ?: "default"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _players.value = playerList
                } catch (e: Exception) {
                    _errorMessage.value = "Error al procesar datos: ${e.message}"
                }
            } else {
                _errorMessage.value = "No se encontró el tablero con ID $boardId"
            }
        }
    }

    /**
     * Cambia el turno al siguiente jugador. Si el turno actual es el 2, reinicia a 1.
     *
     * @param boardId ID del tablero a actualizar.
     */
    fun switchTurn(boardId: String) {
        val boardRef = db.collection("gameBoards").document(boardId)

        boardRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentTurn = document.getLong("currentPlayerIndex")?.toInt() ?: 1
                val newTurn = if (currentTurn == 2) 1 else currentTurn + 1

                boardRef.update("currentPlayerIndex", newTurn)
                    .addOnSuccessListener {
                        _errorMessage.value = null
                    }
                    .addOnFailureListener { exception ->
                        _errorMessage.value = "Error al cambiar el turno: ${exception.message}"
                    }
            } else {
                _errorMessage.value = "No se encontró el tablero con el ID especificado"
            }
        }.addOnFailureListener { exception ->
            _errorMessage.value = "Error al obtener el tablero: ${exception.message}"
        }
    }

    /**
     * [EN DESARROLLO] Ejecuta un movimiento en una columna del tablero.
     * Este método está actualmente incompleto.
     *
     * @param boardId ID del tablero donde se realiza la jugada.
     * @param column Número de columna donde se desea colocar la ficha.
     * @param currentPlayer Jugador que realiza el movimiento.
     */
    fun makeMove(boardId: String, column: Int, currentPlayer: Player) {
        val boardRef = db.collection("gameBoards").document(boardId)

        boardRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Lógica pendiente
            }
        }.addOnFailureListener { exception ->
            _errorMessage.value = "Error al obtener el tablero: ${exception.message}"
        }
    }

    /**
     * [EN DESARROLLO] Verifica si hay un ganador después de un movimiento.
     *
     * @param boardId ID del tablero.
     * @param column Columna afectada por el movimiento.
     */
    private fun checkForWinner(boardId: String, column: Int) {
        val boardRef = db.collection("gameBoards").document(boardId)

        boardRef.get().addOnSuccessListener { document ->
            val board = document.toObject(Board::class.java) ?: return@addOnSuccessListener

            // Lógica pendiente para detectar 4 en línea
        }
    }

    /**
     * Limpia los listeners activos cuando se destruye el ViewModel para evitar fugas de memoria.
     */
    override fun onCleared() {
        super.onCleared()
        boardListener?.remove()
    }
}

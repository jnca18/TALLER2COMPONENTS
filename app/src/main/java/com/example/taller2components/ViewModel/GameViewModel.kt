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


class GameViewModel : ViewModel() {
    private val db = Firebase.firestore
    private var boardListener: ListenerRegistration? = null
    private var playersListener: ListenerRegistration? = null

    // Exponemos el estado de los jugadores y errores
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _board = MutableStateFlow<Board?>(null)
    val board: StateFlow<Board?> = _board

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Función para consultar las propiedades del tablero
     */
    /**
     * Función para consultar las propiedades del tablero con manejo de estados de carga
     */
    fun consultarTablero(idBoard: String) {
        Log.d("consultarTablero", "Consultando tablero: $idBoard")
        _isLoading.value = true
        _errorMessage.value = null

        val boardRef = db.collection("gameBoards").document(idBoard)

        // Remove any existing listener to avoid duplicates
        boardListener?.remove()

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

                        // Procesamiento en un bloque try-catch separado para mejor trazabilidad
                        val board = try {
                            parseBoardData(data)
                        } catch (e: Exception) {
                            throw Exception("Error procesando datos del tablero: ${e.message}", e)
                        }

                        Log.d("consultarTablero", "Tablero actualizado: ${board}")
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
     * Función auxiliar para parsear los datos del tablero
     */
    private fun parseBoardData(data: Map<String, Any>): Board {
        // Campos básicos del tablero
        val id = data["id"] as? String ?: ""
        val rows = (data["rows"] as? Long)?.toInt() ?: 6
        val columns = (data["columns"] as? Long)?.toInt() ?: 7
        val gameStatus = data["gameStatus"] as? String ?: "waiting"
        val state = data["state"] as? Boolean ?: false
        val winner = data["winner"] as? String
        // Procesamiento de jugadores con manejo de errores individual
        val playersRaw = data["players"] as? List<Map<String, Any>> ?: emptyList()
        val players = playersRaw.mapNotNull { playerData ->
            try {
                Player(
                    idPlayer = playerData["idPlayer"] as? String ?: "",
                    correo = playerData["correo"] as? String ?: "",
                    color = playerData["color"] as? String ?: "#000000", // Valor por defecto
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
     * Función para escuchar a los jugadores del tablero
     */
    fun listenToPlayers(boardId: String) {
        val boardRef = db.collection("gameBoards").document(boardId)

        // Si ya hay un listener activo, lo removemos
        playersListener?.remove()

        playersListener = boardRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Error al escuchar cambios: ${error.message}"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    // Obtenemos la lista de jugadores y la convertimos a objetos Player
                    val players2 = snapshot.get("players") as? List<Map<String, Any>> ?: emptyList()
                    val playerList = players2.mapNotNull { data ->
                        try {
                            Log.d("currentUserId", data.toString())
                            Player(
                                idPlayer = data["idPlayer"] as? String ?: "",
                                correo = data["correo"] as? String ?: "",
                                color = data["color"] as? String ?: "default", // Valor por defecto
                            )
                        } catch (e: Exception) {
                            null // Ignoramos entradas corruptas
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

    fun switchTurn(boardId: String) {
        val boardRef = db.collection("gameBoards").document(boardId)

        // Obtenemos el documento del tablero
        boardRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentTurn =
                    document.getLong("currentPlayerIndex")?.toInt() ?: 1  // Obtener turno actual

                // Validar si el turno actual es igual al número de jugadores
                val newTurn = if (currentTurn == 2) {
                    1  // Si el turno es igual al número de jugadores, reinicia a 1
                } else {
                    currentTurn + 1  // Sino, incrementa el turno
                }

                // Actualizamos el turno en el documento
                boardRef.update("currentPlayerIndex", newTurn)
                    .addOnSuccessListener {
                        _errorMessage.value = null  // Limpiar mensaje de error
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

    fun makeMove(boardId: String, column: Int, currentPlayer: Player) {
        val boardRef = db.collection("gameBoards").document(boardId)

        boardRef.get().addOnSuccessListener { document ->
            if (document.exists()) {

            }


        }.addOnFailureListener { exception ->
            _errorMessage.value = "Error al obtener el tablero: ${exception.message}"
        }
    }

    // Función auxiliar para verificar ganador
    private fun checkForWinner(boardId: String, column: Int) {
        val boardRef = db.collection("gameBoards").document(boardId)

        boardRef.get().addOnSuccessListener { document ->
            val board = document.toObject(Board::class.java) ?: return@addOnSuccessListener

            // Lógica para verificar si hay 4 en raya


        }
    }

    /**
     * Limpia los listeners cuando el ViewModel es destruido
     */
    override fun onCleared() {
        super.onCleared()
        boardListener?.remove()
    }

}


package com.example.taller2components.ViewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taller2components.persistence.Board
import com.example.taller2components.persistence.Casilla
import com.example.taller2components.persistence.Player
import com.example.taller2components.persistence.Pregunta
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

    fun consultarTablero(idBoard: String) {
        val boardRef = db.collection("gameBoards").document(idBoard)

        _isLoading.value = true
        _errorMessage.value = null

        boardListener?.remove() // Limpia listeners antiguos

        boardListener = boardRef.addSnapshotListener { snapshot, error ->
            _isLoading.value = false

            if (error != null) {
                _errorMessage.value = "Error al escuchar cambios: ${error.message}"
                Log.e("consultarTablero", "Error en snapshot", error)
                return@addSnapshotListener
            }

            if (snapshot == null || !snapshot.exists()) {
                _errorMessage.value = "No se encontró el tablero con ID: $idBoard"
                return@addSnapshotListener
            }

            try {
                val data = snapshot.data!!
                Log.d("consultarTablero", "Datos recibidos: ${data.keys}")

                val id = data["id"] as? String ?: idBoard
                val rows = (data["rows"] as? Long)?.toInt() ?: 6
                val columns = (data["columns"] as? Long)?.toInt() ?: 7
                val gameStatus = data["gameStatus"] as? String ?: "waiting"
                val state = data["state"] as? Boolean ?: false
                val winner = data["winner"] as? String
                val currentPlayerIndex = (data["currentPlayerIndex"] as? Long)?.toInt() ?: 1

                val playersRaw = data["players"] as? List<Map<String, Any>> ?: emptyList()
                val players = playersRaw.mapNotNull { player ->
                    try {
                        Player(
                            idPlayer = player["idPlayer"] as? String ?: "",
                            correo = player["correo"] as? String ?: "",
                            color = player["color"] as? String ?: "#000000",
                            score = (player["score"] as? Long)?.toInt() ?: 0,
                            isCurrentTurn = player["isCurrentTurn"] as? Boolean ?: false,
                            wins = (player["wins"] as? Long)?.toInt() ?: 0,
                            isHost = player["isHost"] as? Boolean ?: false,
                            isWinner = player["isWinner"] as? Boolean ?: false,
                            turno = (player["turno"] as? Long)?.toInt() ?: 1
                        )
                    } catch (e: Exception) {
                        Log.e("consultarTablero", "Error en jugador: $player", e)
                        null
                    }
                }

                val gridRaw = data["grid"] as? List<Map<String, Any?>> ?: emptyList()
                val grid = gridRaw.mapNotNull { cell ->
                    try {
                        val fila = (cell["fila"] as? Long)?.toInt() ?: return@mapNotNull null
                        val columna = (cell["columna"] as? Long)?.toInt() ?: return@mapNotNull null
                        val valor = (cell["valor"] as? Long)?.toInt() ?: 0
                        val color = cell["color"] as? String ?: ""
                        Casilla(fila, columna, valor, color)
                    } catch (e: Exception) {
                        Log.e("consultarTablero", "Error parseando casilla: $cell", e)
                        null
                    }
                }

                val board = Board(
                    id = id,
                    rows = rows,
                    columns = columns,
                    players = players,
                    state = state,
                    gameStatus = gameStatus,
                    winner = winner,
                    currentPlayerIndex = currentPlayerIndex,
                    grid = grid
                )

                Log.d("consultarTablero", "Tablero parseado correctamente")

                _board.value = board

            } catch (e: Exception) {
                _errorMessage.value = "Error al procesar datos: ${e.message}"
                Log.e("consultarTablero", "Excepción general", e)
            }
        }
    }

    fun switchTurn(boardId: String) {
        val boardRef = db.collection("gameBoards").document(boardId)

        // Obtenemos el documento del tablero
        boardRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentTurn = document.getLong("currentPlayerIndex")?.toInt() ?: 1  // Obtener turno actual

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
                val data = document.data ?: return@addOnSuccessListener
                val rows = (data["rows"] as? Long)?.toInt() ?: 6
                val columns = (data["columns"] as? Long)?.toInt() ?: 7
                Log.d("makeMove","prueba1")

                val gridRaw = data["grid"] as? List<Map<String, Any?>> ?: emptyList()
                Log.d("makeMove", "gridRaw size: ${gridRaw.size} => $gridRaw")

                val grid = gridRaw.mapNotNull { item ->
                    Log.d("makeMove",item.toString() )

                    try {
                        val fila = (item["fila"] as? Long)?.toInt() ?: return@mapNotNull null
                        val columna = (item["columna"] as? Long)?.toInt() ?: return@mapNotNull null
                        val valor = (item["valor"] as? Long)?.toInt() ?: 0
                        val color = item["color"] as? String ?: ""

                        Casilla(fila, columna, valor, color)
                    } catch (e: Exception) {
                        Log.e("makeMove", "Error al parsear casilla: $item", e)
                        null
                    }
                }.toMutableList()

                Log.d("makeMove",grid.toString() )


                // Buscar desde la última fila hacia arriba
                val filaObjetivo = (rows - 1 downTo 0).firstOrNull { fila ->
                    grid.any { it.fila == fila && it.columna == column && it.valor == 0 }
                }

                if (filaObjetivo != null) {
                    val index = grid.indexOfFirst { it.fila == filaObjetivo && it.columna == column }
                    if (index != -1) {
                        grid[index] = Casilla(
                            fila = filaObjetivo,
                            columna = column,
                            valor = currentPlayer.turno,
                            color = currentPlayer.color
                        )
                        Log.d("makeMove",grid[index].toString() )
                        // Actualizar Firestore con la nueva grilla
                        boardRef.update("grid", grid.map {
                            mapOf(
                                "fila" to it.fila,
                                "columna" to it.columna,
                                "valor" to it.valor,
                                "color" to it.color
                            )
                        }).addOnSuccessListener {
                            Log.d("makeMove", "Movimiento realizado correctamente")
                            checkForWinner(boardId, column, currentPlayer.turno)
                        }.addOnFailureListener {
                            _errorMessage.value = "Error al actualizar el tablero: ${it.message}"
                        }
                    }
                } else {
                    _errorMessage.value = "Columna llena"
                }
            } else {
                _errorMessage.value = "El tablero no existe"
            }
        }.addOnFailureListener { exception ->
            _errorMessage.value = "Error al obtener el tablero: ${exception.message}"
        }
    }


    private fun checkForWinner(boardId: String, columnPlayed: Int, jugador: Int, ) {
        val boardRef = db.collection("gameBoards").document(boardId)

        boardRef.get().addOnSuccessListener { document ->
            if (!document.exists()) return@addOnSuccessListener

            val data = document.data ?: return@addOnSuccessListener
            val rows = (data["rows"] as? Long)?.toInt() ?: 6
            val columns = (data["columns"] as? Long)?.toInt() ?: 7
            val gridRaw = data["grid"] as? List<Map<String, Any?>> ?: return@addOnSuccessListener

            val grid = gridRaw.mapNotNull { item ->
                try {
                    val fila = (item["fila"] as? Long)?.toInt() ?: return@mapNotNull null
                    val columna = (item["columna"] as? Long)?.toInt() ?: return@mapNotNull null
                    val valor = (item["valor"] as? Long)?.toInt() ?: 0
                    val color = item["color"] as? String ?: ""
                    Casilla(fila, columna, valor, color)
                } catch (e: Exception) {
                    null
                }
            }

            // Convertir a matriz
            val matriz = Array(rows) { fila ->
                Array(columns) { columna ->
                    grid.find { it.fila == fila && it.columna == columna } ?: Casilla(fila, columna, 0, "")
                }
            }

            // Verificar si hay 4 en raya
            fun contarConsecutivos(f: Int, c: Int, df: Int, dc: Int, turno: Int): Int {
                var count = 0
                var fila = f
                var col = c

                while (fila in 0 until rows && col in 0 until columns && matriz[fila][col].valor == turno) {
                    count++
                    fila += df
                    col += dc
                }
                return count
            }


            // Buscar última fila jugada en esa columna
            val filaJugada = (rows - 1 downTo 0).firstOrNull {
                matriz[it][columnPlayed].valor == jugador
            } ?: return@addOnSuccessListener

            val direcciones = listOf(
                Pair(0, 1),   // Horizontal →
                Pair(1, 0),   // Vertical ↓
                Pair(1, 1),   // Diagonal ↘
                Pair(1, -1)   // Diagonal ↙
            )

            for ((df, dc) in direcciones) {
                val total = contarConsecutivos(filaJugada, columnPlayed, df, dc, jugador - 0) +
                        contarConsecutivos(filaJugada - df, columnPlayed - dc, -df, -dc, jugador) - 1

                if (total >= 3) {
                    // ¡Ganador encontrado!
                    boardRef.update("winner", jugador.toString(), "state", false)
                        .addOnSuccessListener {
                            Log.d("checkForWinner", "Ganador actualizado: Jugador $jugador")
                        }
                        .addOnFailureListener {
                            Log.e("checkForWinner", "Error al actualizar ganador: ${it.message}")
                        }
                    return@addOnSuccessListener
                }
            }

        }.addOnFailureListener {
            Log.e("checkForWinner", "Error obteniendo tablero: ${it.message}")
        }
    }

    fun consultarPregunta(idPregunta: String, onResult: (Pregunta?) -> Unit) {
        val db = Firebase.firestore

        db.collection("preguntas")
            .document(idPregunta.toString())
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val data = document.data ?: return@addOnSuccessListener onResult(null)
                        val pregunta = data["pregunta"] as? String ?: return@addOnSuccessListener onResult(null)
                        val opciones = data["opciones"] as? List<String> ?: return@addOnSuccessListener onResult(null)
                        val respuesta = (data["respuesta"] as? Long)?.toInt() ?: return@addOnSuccessListener onResult(null)

                        onResult(Pregunta(pregunta, opciones, respuesta))
                    } catch (e: Exception) {
                        Log.e("consultarPregunta", "Error al parsear pregunta", e)
                        onResult(null)
                    }
                } else {
                    Log.w("consultarPregunta", "No se encontró la pregunta con ID: $idPregunta")
                    onResult(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("consultarPregunta", "Error al consultar la pregunta", exception)
                onResult(null)
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

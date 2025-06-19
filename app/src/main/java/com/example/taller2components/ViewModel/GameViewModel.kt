package ud.example.four_in_row.ViewModel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ud.example.four_in_row.persistence.Player

class GameViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var playersListener: ListenerRegistration? = null
    private var gameListener: ListenerRegistration? = null

    private var _isLoading = MutableStateFlow<Boolean>(false)
    val idLoading: StateFlow<String?> = _isLoading

    // Estados del juego
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _gameGrid = MutableStateFlow<List<List<String>>>(List(6) { List(7) { "" } })
    val gameGrid: StateFlow<List<List<String>>> = _gameGrid

    private val _currentPlayerTurn = MutableStateFlow<Player?>(null)
    val currentPlayerTurn: StateFlow<Player?> = _currentPlayerTurn

    private val _gameStatus = MutableStateFlow("waiting") // waiting, playing, finished
    val gameStatus: StateFlow<String> = _gameStatus

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage



    // Crear un nuevo tablero de 4 en raya
    fun createGameBoard(hostPlayer: Player): String {
        val boardId = db.collection("fourInRowGames").document().id

        val initialGrid = List(6) { List(7) { "" } }

        val gameData = hashMapOf(
            "rows" to 6,
            "columns" to 7,
            "grid" to initialGrid,
            "players" to listOf(hostPlayer.toMap()),
            "currentPlayerIndex" to 0,
            "status" to "waiting",
            "winner" to null
        )

        db.collection("fourInRowGames").document(boardId)
            .set(gameData)
            .addOnSuccessListener {
                _errorMessage.value = null
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al crear el juego: ${e.message}"
            }

        return boardId
    }

    // Realizar un movimiento
    fun makeMove(boardId: String, column: Int) {
        if (column < 0 || column >= 7) {
            _errorMessage.value = "Columna inválida"
            return
        }

        db.runTransaction { transaction ->
            val gameRef = db.collection("fourInRowGames").document(boardId)
            val gameDoc = transaction.get(gameRef)

            val grid = gameDoc.get("grid") as List<List<String>>
            val players = (gameDoc.get("players") as List<Map<String, Any>>).map { Player.fromMap(it) }
            val currentIndex = gameDoc.getLong("currentPlayerIndex")?.toInt() ?: 0
            val status = gameDoc.getString("status") ?: "waiting"

            if (status != "playing") {
                throw Exception("El juego no está en progreso")
            }

            // Encontrar primera fila vacía en la columna
            val mutableGrid = grid.map { it.toMutableList() }.toMutableList()
            var row = -1
            for (i in 5 downTo 0) {
                if (mutableGrid[i][column].isEmpty()) {
                    mutableGrid[i][column] = players[currentIndex].color
                    row = i
                    break
                }
            }

            if (row == -1) {
                throw Exception("Columna llena")
            }

            // Verificar si hay ganador
            val winner = checkWinner(mutableGrid, row, column, players[currentIndex])

            // Actualizar datos
            val nextPlayerIndex = (currentIndex + 1) % players.size
            val newStatus = if (winner != null) "finished" else status

            transaction.update(gameRef, mapOf(
                "grid" to mutableGrid,
                "currentPlayerIndex" to nextPlayerIndex,
                "status" to newStatus,
                "winner" to winner?.correo
            ))

            // Actualizar estadísticas del jugador
            if (winner != null) {
                val playerRef = db.collection("players").document(winner.idPlayer)
                transaction.update(playerRef,
                    mapOf(
                        "wins" to FieldValue.increment(1),
                        "score" to FieldValue.increment(100)
                    )
                )
            }
        }.addOnSuccessListener {
            _errorMessage.value = null
        }.addOnFailureListener { e ->
            _errorMessage.value = "Error al realizar movimiento: ${e.message}"
        }
    }

    // Escuchar cambios en el juego
    fun listenToGame(boardId: String) {
        gameListener = db.collection("fourInRowGames").document(boardId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _errorMessage.value = "Error al escuchar juego: ${error.message}"
                    return@addSnapshotListener
                }

                snapshot?.let { doc ->
                    _gameGrid.value = doc.get("grid") as? List<List<String>> ?: List(6) { List(7) { "" } }

                    val playersList = (doc.get("players") as? List<Map<String, Any>>)?.map { Player.fromMap(it) } ?: emptyList()
                    _players.value = playersList

                    val currentIndex = doc.getLong("currentPlayerIndex")?.toInt() ?: 0
                    if (playersList.isNotEmpty() && currentIndex < playersList.size) {
                        _currentPlayerTurn.value = playersList[currentIndex]
                    }

                    _gameStatus.value = doc.getString("status") ?: "waiting"
                }
            }
    }

    // Iniciar el juego
    fun startGame(boardId: String) {
        db.collection("fourInRowGames").document(boardId)
            .update(mapOf(
                "status" to "playing",
                "currentPlayerIndex" to 0
            ))
            .addOnSuccessListener {
                _errorMessage.value = null
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al iniciar juego: ${e.message}"
            }
    }


    // Verificar ganador
    private fun checkWinner(grid: List<List<String>>, row: Int, col: Int, player: Player): Player? {
        val color = player.color
        val directions = listOf(
            Pair(0, 1),  // Horizontal
            Pair(1, 0),  // Vertical
            Pair(1, 1),  // Diagonal \
            Pair(1, -1)  // Diagonal /
        )

        for ((dx, dy) in directions) {
            var count = 1

            // Verificar en dirección positiva
            var x = row + dx
            var y = col + dy
            while (x in 0..5 && y in 0..6 && grid[x][y] == color) {
                count++
                x += dx
                y += dy
            }

            // Verificar en dirección negativa
            x = row - dx
            y = col - dy
            while (x in 0..5 && y in 0..6 && grid[x][y] == color) {
                count++
                x -= dx
                y -= dy
            }

            if (count >= 4) {
                return player
            }
        }

        return null
    }

    override fun onCleared() {
        super.onCleared()
        playersListener?.remove()
        gameListener?.remove()
    }
}

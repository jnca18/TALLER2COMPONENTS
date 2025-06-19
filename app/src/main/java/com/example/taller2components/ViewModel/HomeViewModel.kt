package ud.example.four_in_row.ViewModel
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ud.example.four_in_row.persistence.Operaciones
import ud.example.four_in_row.persistence.Player

class GameBoardViewModel : ViewModel() {

    private val db = Firebase.firestore
    private var playersListener: ListenerRegistration? = null

    // Exponemos el estado de los jugadores y errores
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _currentPlayerTurn = MutableStateFlow<Player?>(null)
    val currentPlayerTurn: StateFlow<Player?> = _currentPlayerTurn

    fun createGameBoard(hostPlayer: Player): String {
        // Generate game ID
        val boardId = db.collection("fourInRowGames").document().id

        // Create initial empty grid (6 rows x 7 columns)
        val initialGrid = List(6) { List(7) { "" } }

        // Create game data structure
        val gameBoard = hashMapOf(
            "id" to boardId,
            "rows" to 6,
            "columns" to 7,
            "grid" to initialGrid,
            "players" to listOf(hostPlayer.toMap()),
            "currentPlayerIndex" to 0,
            "status" to "waiting", // waiting, playing, finished
            "winner" to null,
            "state" to false
        )

        // Save to Firestore
        db.collection("fourInRowGames").document(boardId)
            .set(gameBoard)
            .addOnSuccessListener {
                // Successfully created game
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error creating game: ${e.message}"
            }

        return boardId
    }

    private fun startGame(boardId: String) {
        val boardRef = db.collection("fourInRowGames").document(boardId)

        // Actualizaciones para iniciar el juego
        val updates = hashMapOf<String, Any>(
            "status" to "playing",
            "currentPlayerIndex" to 0, // El host (primer jugador) empieza
            "grid" to List(6) { List(7) { "" } } // Tablero vacío
        )

        boardRef.update(updates)
            .addOnSuccessListener {
                _errorMessage.value = null
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al iniciar juego: ${e.message}"
            }
    }

    /**
     * Función para agregar un jugador al tablero de 4 en raya (máximo 2 jugadores)
     */
    fun joinBoard(boardId: String, player: Player) {
        val boardRef = db.collection("fourInRowGames").document(boardId)

        boardRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    _errorMessage.value = "No se encontró el tablero con ID: $boardId"
                    return@addOnSuccessListener
                }

                // Obtener lista actual de jugadores
                val currentPlayers = document.get("players") as? List<Map<String, Any>> ?: emptyList()

                // Verificar si el jugador ya está en el juego
                if (currentPlayers.any { it["idPlayer"] == player.idPlayer }) {
                    _errorMessage.value = "Ya estás en este juego"
                    return@addOnSuccessListener
                }

                // Limitar a 2 jugadores máximo
                if (currentPlayers.size >= 2) {
                    _errorMessage.value = "El juego ya tiene 2 jugadores (máximo permitido)"
                    return@addOnSuccessListener
                }

                // Crear datos del jugador para Firestore
                val playerData = player.copy(
                    isHost = currentPlayers.isEmpty(), // El primer jugador es el host
                    isCurrentTurn = currentPlayers.isEmpty() // El host empieza primero
                ).toMap()

                // Añadir jugador al tablero
                boardRef.update(
                    "players", FieldValue.arrayUnion(playerData),
                    "status", if (currentPlayers.size == 1) "playing" else "waiting"
                )
                    .addOnSuccessListener {
                        _errorMessage.value = null
                        // Si es el segundo jugador, iniciar el juego
                        if (currentPlayers.size == 1) {
                            startGame(boardId)
                        }
                    }
                    .addOnFailureListener { e ->
                        _errorMessage.value = "Error al unirse al juego: ${e.message}"
                    }
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al acceder al tablero: ${e.message}"
            }
    }


    /**
     * Escuchar cambios en la lista de jugadores (máximo 2 jugadores para 4 en raya)
     */
    fun listenToPlayers(boardId: String) {
        val boardRef = db.collection("fourInRowGames").document(boardId)

        // Remover listener anterior si existe
        playersListener?.remove()

        playersListener = boardRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _errorMessage.value = "Error al escuchar jugadores: ${error.message}"
                return@addSnapshotListener
            }

            snapshot?.let { doc ->
                // Obtener lista de jugadores y convertir a objetos Player
                val playersData = doc.get("players") as? List<Map<String, Any>> ?: emptyList()

                // Limitar a 2 jugadores máximo
                if (playersData.size > 2) {
                    _errorMessage.value = "4 en raya solo permite 2 jugadores"
                    return@addSnapshotListener
                }

                val playerList = playersData.map { data ->
                    Player.fromMap(data).apply {
                        // Asignar turno inicial (primer jugador empieza)
                        if (playersData.indexOf(data) == 0) {
                            isCurrentTurn = true
                        }
                    }
                }

                _players.value = playerList

                // Actualizar jugador actual si corresponde
                val currentIndex = doc.getLong("currentPlayerIndex")?.toInt() ?: 0
                if (currentIndex < playerList.size) {
                    _currentPlayerTurn.value = playerList[currentIndex]
                }
            }
        }
    }

    /**
     * Actualiza el estado del tablero para 4 en raya
     * @param boardId ID del tablero a actualizar
     */
    fun updateBoardState(boardId: String) {
        val boardRef = db.collection("fourInRowGames").document(boardId)

        // Actualizaciones para el juego de 4 en raya
        val updates = hashMapOf<String, Any>(
            "status" to "playing",  // Cambiar estado a "en juego"
            "grid" to List(6) { List(7) { "" } },  // Inicializar tablero vacío
            "currentPlayerIndex" to 0  // Primer jugador empieza
        ).apply {
            // Añadir winner como null solo si es necesario
            (null as Any?)?.let { put("winner", it) }  // Solución explícita para el tipo
        }

        // Actualizar el documento en Firestore
        boardRef.update(updates)
            .addOnSuccessListener {
                _errorMessage.value = null
            }
            .addOnFailureListener { exception ->
                _errorMessage.value = "Error al actualizar tablero: ${exception.message}"
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

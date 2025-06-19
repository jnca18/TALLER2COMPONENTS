package ud.example.four_in_row.persistence

data class Player(
    val idPlayer: String = "",
    val correo: String = "",
    val color: String = generateRandomColor(),
    var score: Int = 0,
    var isCurrentTurn: Boolean = false,
    var wins: Int = 0,
    val isHost: Boolean = false,
    var lastMove: Pair<Int, Int>? = null,
    val isWinner: Boolean = false // Añade esta propiedad
) {
    // Converts Player to Map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "idPlayer" to idPlayer,
            "correo" to correo,
            "color" to color,
            "score" to score,
            "isCurrentTurn" to isCurrentTurn,
            "wins" to wins,
            "isHost" to isHost,
            "lastMoveRow" to (lastMove?.first ?: -1),
            "lastMoveCol" to (lastMove?.second ?: -1)
        )
    }

    companion object {
        // Creates Player from Firestore Map
        fun fromMap(map: Map<String, Any>): Player {
            return Player(
                idPlayer = map["idPlayer"] as? String ?: "",
                correo = map["correo"] as? String ?: "",
                color = map["color"] as? String ?: generateRandomColor(),
                score = (map["score"] as? Long)?.toInt() ?: 0,
                isCurrentTurn = map["isCurrentTurn"] as? Boolean ?: false,
                wins = (map["wins"] as? Long)?.toInt() ?: 0,
                isHost = map["isHost"] as? Boolean ?: false,
                lastMove = Pair(
                    (map["lastMoveRow"] as? Long)?.toInt() ?: -1,
                    (map["lastMoveCol"] as? Long)?.toInt() ?: -1
                ).takeIf { it.first != -1 && it.second != -1 }
            )
        }

        private fun generateRandomColor(): String {
            val r = (0..255).random()
            val g = (0..255).random()
            val b = (0..255).random()
            return String.format("#%02x%02x%02x", r, g, b)
        }
    }

    fun resetForNewGame() {
        lastMove = null
        isCurrentTurn = false
    }

    fun updateAfterMove(row: Int, col: Int, isWin: Boolean = false) {
        lastMove = Pair(row, col)
        if (isWin) {
            wins++
            score += 100
        } else {
            score += 10
        }
    }
}
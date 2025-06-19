package ud.example.four_in_row.persistence

data class Casilla(
    val valor: Int = 0,
    val color: String = "" // Color hexadecimal del jugador que ocupa la casilla
) {
    // Posición en el tablero (opcional, puede ser útil)
    var row: Int = -1
    var column: Int = -1
}
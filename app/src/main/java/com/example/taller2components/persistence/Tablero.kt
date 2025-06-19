package ud.example.four_in_row.persistence

object Tablero {
    private var idTablero: String = ""
    private var tablero: MutableList<MutableList<Casilla>> = mutableListOf()

    fun iniciarTablero(board: Board): List<List<Casilla>> {
        idTablero = board.id

        // Inicializar el tablero vacío para 4 en raya (6 filas x 7 columnas)
        if (tablero.isEmpty()) {
            tablero = MutableList(board.rows) {
                MutableList(board.columns) { Casilla(0) }
            }
        }

        // En 4 en raya no necesitamos posiciones iniciales de jugadores
        // como en serpientes y escaleras, así que eliminamos esa parte

        return tablero
    }

    // Función para realizar un movimiento en el tablero
    fun hacerMovimiento(columna: Int, colorJugador: String): Pair<Int, Int>? {
        if (columna < 0 || columna >= tablero[0].size) return null

        // Encontrar la primera fila vacía en la columna seleccionada
        for (fila in tablero.size - 1 downTo 0) {
            if (tablero[fila][columna].valor == 0) {
                tablero[fila][columna] = Casilla(1, colorJugador)
                return Pair(fila, columna)
            }
        }

        return null // Columna llena
    }

    // Función para verificar si hay un ganador
    fun verificarGanador(fila: Int, columna: Int, color: String): Boolean {
        val directions = listOf(
            Pair(0, 1),   // Horizontal
            Pair(1, 0),    // Vertical
            Pair(1, 1),    // Diagonal \
            Pair(1, -1)    // Diagonal /
        )

        for ((dx, dy) in directions) {
            var count = 1

            // Verificar en dirección positiva
            var x = fila + dx
            var y = columna + dy
            while (x in tablero.indices && y in tablero[0].indices &&
                tablero[x][y].color == color) {
                count++
                x += dx
                y += dy
            }

            // Verificar en dirección negativa
            x = fila - dx
            y = columna - dy
            while (x in tablero.indices && y in tablero[0].indices &&
                tablero[x][y].color == color) {
                count++
                x -= dx
                y -= dy
            }

            if (count >= 4) return true
        }

        return false
    }

    // Reiniciar el tablero
    fun reiniciar() {
        for (fila in tablero.indices) {
            for (columna in tablero[fila].indices) {
                tablero[fila][columna] = Casilla(0)
            }
        }
    }
}
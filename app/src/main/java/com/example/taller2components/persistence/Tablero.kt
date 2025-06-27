package com.example.taller2components.persistence

/**
 * Objeto singleton que gestiona la lógica y estado del tablero en el juego 4 en línea.
 *
 * Contiene funciones para inicializar el tablero, realizar movimientos y verificar condiciones de victoria.
 */
object Tablero {
    // Identificador del tablero actual (útil si se manejan múltiples partidas)
    private var idTablero: String = ""

    // Matriz del tablero donde se almacenan las casillas con su estado
    private var tablero: MutableList<MutableList<Casilla>> = mutableListOf()

    /**
     * Inicializa un nuevo tablero vacío con base en las dimensiones del objeto [Board].
     * Solo se inicializa si aún no se ha creado uno.
     *
     * @param board Objeto que contiene la configuración inicial del tablero.
     * @return La matriz bidimensional del tablero ya inicializada.
     */
    fun iniciarTablero(board: Board): List<List<Casilla>> {
        idTablero = board.id

        // Crear el tablero vacío si aún no se ha inicializado
        if (tablero.isEmpty()) {
            tablero = MutableList(board.rows) {
                MutableList(board.columns) { Casilla(0) }
            }
        }

        return tablero
    }

    /**
     * Intenta colocar una ficha en la columna indicada por el jugador.
     *
     * La ficha caerá a la primera fila vacía desde abajo hacia arriba.
     *
     * @param columna Índice de la columna donde el jugador desea colocar su ficha.
     * @param valor Valor numérico que representa al jugador (1 o 2).
     * @param colorJugador Color asignado al jugador (usado para visualización).
     * @return Par (fila, columna) donde se colocó la ficha, o `null` si la columna está llena o fuera de rango.
     */
    fun hacerMovimiento(columna: Int, valor: Int, colorJugador: String): Pair<Int, Int>? {
        if (columna < 0 || columna >= tablero[0].size) return null

        // Buscar la primera fila vacía en la columna
        for (fila in tablero.size - 1 downTo 0) {
            if (tablero[fila][columna].valor == 0) {
                tablero[fila][columna] = Casilla(fila, columna, valor, colorJugador)
                return Pair(fila, columna)
            }
        }

        return null // Columna llena
    }

    /**
     * Verifica si existe una línea de 4 fichas consecutivas del mismo color a partir de la última jugada.
     *
     * Revisa en 4 direcciones: horizontal, vertical, y dos diagonales.
     *
     * @param fila Fila donde se realizó el último movimiento.
     * @param columna Columna del último movimiento.
     * @param color Color del jugador que hizo la jugada.
     * @return `true` si hay 4 en línea consecutivos, `false` de lo contrario.
     */
    fun verificarGanador(fila: Int, columna: Int, color: String): Boolean {
        val directions = listOf(
            Pair(0, 1),   // Horizontal →
            Pair(1, 0),   // Vertical ↓
            Pair(1, 1),   // Diagonal ↘
            Pair(1, -1)   // Diagonal ↙
        )

        for ((dx, dy) in directions) {
            var count = 1

            // Avanza en dirección positiva
            var x = fila + dx
            var y = columna + dy
            while (x in tablero.indices && y in tablero[0].indices &&
                tablero[x][y].color == color) {
                count++
                x += dx
                y += dy
            }

            // Avanza en dirección contraria
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
}
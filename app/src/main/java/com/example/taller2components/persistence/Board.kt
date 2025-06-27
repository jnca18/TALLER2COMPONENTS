package com.example.taller2components.persistence
/**
 * Representa el estado general de un tablero de juego para 4 en línea.
 *
 * @property id Identificador único del tablero, útil para sesiones multijugador o persistencia.
 * @property rows Número de filas del tablero. Por defecto es 6, según el estándar de 4 en línea.
 * @property columns Número de columnas del tablero. Por defecto es 7.
 * @property players Lista de jugadores que participan en la partida.
 * @property grid Matriz bidimensional que representa las casillas del tablero.
 * Cada celda contiene una instancia de [Casilla], y está inicializada como una lista de listas vacías.
 * @property state Booleano que indica si la partida ha comenzado (`true`) o aún está esperando jugadores (`false`).
 * @property currentPlayerIndex Índice del jugador al que le corresponde el siguiente turno.
 * @property gameStatus Estado textual del juego, puede ser: `"waiting"`, `"playing"` o `"finished"`.
 * @property winner Correo electrónico del jugador ganador, o `null` si aún no hay ganador.
 */
data class Board(
    val id: String = "",
    val rows: Int = 6,  // Tamaño estándar para 4 en raya: 6 filas x 7 columnas
    val columns: Int = 7,
    val players: List<Player> = emptyList(),
    val grid: List<Casilla> = emptyList(),
    val state: Boolean = false, // Indica si el juego ha comenzado
    val currentPlayerIndex: Int = 1, // Índice del jugador actual
    val gameStatus: String = "waiting", // waiting, playing, finished
    val winner: String? = null // Email del ganador (si lo hay)
)
package com.example.taller2components.persistence

/**
 * Representa una celda individual (casilla) dentro del tablero del juego 4 en línea.
 * Cada casilla contiene su ubicación y el estado actual (ocupada o vacía).
 *
 * @property fila Número de fila donde se encuentra la casilla (0-indexado).
 * @property columna Número de columna donde se encuentra la casilla (0-indexado).
 * @property valor Entero que indica el estado de la casilla:
 * - 0: casilla vacía
 * - 1: ocupada por el jugador 1
 * - 2: ocupada por el jugador 2
 * @property color Representación en texto del color de la ficha (ej: "red", "yellow").
 * Utilizado principalmente para renderizar la interfaz visual del juego.
 */
data class Casilla(
    val fila: Int = 0,
    val columna: Int = 0,
    val valor: Int = 0, // 0 = vacío, 1/2 = jugador
    val color: String = "",
)
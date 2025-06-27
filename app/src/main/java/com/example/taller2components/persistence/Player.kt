package com.example.taller2components.persistence

/**
 * Representa a un jugador dentro del juego 4 en línea, incluyendo su información básica,
 * estado actual dentro de la partida y estadísticas de juego.
 *
 * @property idPlayer Identificador único del jugador, generalmente generado por el sistema.
 * @property correo Dirección de correo electrónico del jugador, usada para identificación o registro.
 * @property color Color asignado al jugador, utilizado para mostrar sus fichas en el tablero.
 * @property score Puntaje actual del jugador en la partida.
 * @property isCurrentTurn Indica si es el turno actual de este jugador.
 * @property wins Número de partidas ganadas por este jugador.
 * @property isHost Indica si el jugador es el anfitrión de la partida.
 * @property lastMove Última jugada realizada, representada como un par (fila, columna).
 * @property isWinner Indica si este jugador ha ganado la partida actual.
 * @property turno Número que representa el orden de turno del jugador (1 o 2).
 */

data class Player(
    val idPlayer: String = "",
    val correo: String = "",
    val color: String,
    var score: Int = 0,
    var isCurrentTurn: Boolean = false,
    var wins: Int = 0,
    val isHost: Boolean = false,
    var lastMove: Pair<Int, Int>? = null,
    val isWinner: Boolean = false,
    val turno: Int = 1
)
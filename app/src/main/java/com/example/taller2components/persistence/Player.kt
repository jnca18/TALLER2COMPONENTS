package com.example.taller2components.persistence

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
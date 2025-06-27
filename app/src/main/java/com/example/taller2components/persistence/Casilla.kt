package com.example.taller2components.persistence

data class Casilla(
    val fila:Int =0,
    val columna: Int = 0,
    val valor: Int = 0, // 0 = vacío, 1/2 = jugador
    val color: String = "",
)
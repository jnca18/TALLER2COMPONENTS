package com.example.taller2components.persistence

data class Pregunta(
    val pregunta: String = "",
    val opciones: List<String> = emptyList(),
    val respuesta: Int = -1
)

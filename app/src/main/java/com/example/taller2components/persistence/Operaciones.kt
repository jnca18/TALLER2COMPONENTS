package com.example.taller2components.persistence

import kotlin.random.Random

/**
 * Clase base que contiene operaciones genéricas utilizadas dentro del juego.
 * Puede ser extendida por otras clases que necesiten funcionalidades comunes.
 */
open class Operaciones {

    /**
     * Genera un color aleatorio en formato hexadecimal.
     *
     * El método crea valores aleatorios para los tres componentes del modelo RGB (Rojo, Verde, Azul),
     * cada uno entre 0 y 255. Luego convierte estos valores en una cadena hexadecimal del tipo "#RRGGBB",
     * útil para establecer colores dinámicos en la interfaz del juego.
     *
     * @return Un string representando un color aleatorio en formato hexadecimal.
     */
    open fun generateRandomColor(): String {
        // Generar valores aleatorios para rojo, verde y azul (RGB)
        val red = Random.nextInt(0, 256) // Valor entre 0 y 255
        val green = Random.nextInt(0, 256)
        val blue = Random.nextInt(0, 256)

        // Convertir los valores RGB a formato hexadecimal (#RRGGBB)
        return String.format("#%02X%02X%02X", red, green, blue)
    }

}
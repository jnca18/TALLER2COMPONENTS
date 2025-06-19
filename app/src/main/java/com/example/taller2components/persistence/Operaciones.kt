package ud.example.four_in_row.persistence

import kotlin.random.Random

open class Operaciones {

    open fun generateRandomColor(): String {
        // Generar valores aleatorios para rojo, verde y azul (RGB)
        val red = Random.nextInt(0, 256) // Valor entre 0 y 255
        val green = Random.nextInt(0, 256)
        val blue = Random.nextInt(0, 256)

        // Convertir los valores RGB a formato hexadecimal (#RRGGBB)
        return String.format("#%02X%02X%02X", red, green, blue)
    }

}

package com.example.taller2components.Enum

/**
 * EnumEstado define los posibles estados que puede tener una casilla
 * dentro del juego de 4 en línea. Estos estados son utilizados para representar
 * visualmente el contenido o la conexión entre fichas en el tablero de juego.
 *
 * Estados disponibles:
 * - EMPTY: Representa una casilla vacía sin ficha.
 * - HEAD: Indica la ficha inicial o principal de una secuencia ganadora.
 * - LINE: Representa una ficha intermedia que forma parte de una línea ganadora.
 * - DOWN: Indica una dirección descendente en una secuencia ganadora.
 * - UP: Indica una dirección ascendente en una secuencia ganadora.
 */
enum class EnumEstado {
    /** Estado vacío sin ficha. */
    EMPTY,

    /** Estado inicial o principal de la secuencia ganadora. */
    HEAD,

    /** Estado que indica una ficha intermedia conectada en línea. */
    LINE,

    /** Estado que indica dirección descendente en la conexión ganadora. */
    DOWN,

    /** Estado que indica dirección ascendente en la conexión ganadora. */
    UP
}
package com.example.taller2components.Enum

/**
 * EnumNavigation define las diferentes pantallas o destinos disponibles
 * dentro de la navegación principal de la aplicación del juego 4 en línea.
 *
 * Valores del enum:
 * - HOME: Pantalla principal o menú de inicio del juego.
 * - PLAY: Pantalla donde se desarrolla el juego principal (tablero de juego).
 * - SETTINGS: Pantalla para ajustar las preferencias y configuraciones del usuario.
 * - LOGIN: Pantalla para autenticación o inicio de sesión del usuario.
 * - ABOUT: Pantalla que muestra información sobre la aplicación, créditos y versión.
 * - EXIT: Opción que permite cerrar o salir de la aplicación.
 */
enum class EnumNavigation {
    /** Pantalla principal o menú inicial. */
    HOME,

    /** Pantalla del tablero donde se juega la partida. */
    PLAY,

    /** Pantalla de configuraciones del usuario o aplicación. */
    SETTINGS,

    /** Pantalla para autenticación e inicio de sesión. */
    LOGIN,

    /** Pantalla con información sobre la aplicación. */
    ABOUT,

    /** Opción para salir o cerrar la aplicación. */
    EXIT
}
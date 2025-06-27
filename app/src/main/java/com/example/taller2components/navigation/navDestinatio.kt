package com.example.taller2components.navigation

import kotlinx.serialization.Serializable

/**
 * Objeto serializable que representa la ruta o pantalla de inicio de sesión
 * dentro del sistema de navegación de la aplicación.
 *
 * Al ser un objeto, no contiene propiedades adicionales, pero su presencia
 * como @Serializable permite su uso en esquemas de rutas o navegación declarativa.
 */
@Serializable
object Login

/**
 * Clase de datos serializable que representa la pantalla o ruta del juego.
 *
 * @property gameId Identificador único de la partida, utilizado para cargar
 * o diferenciar sesiones de juego específicas en la navegación.
 */
@Serializable
data class Game(val gameId: String)

/**
 * Clase de datos serializable que representa la pantalla de inicio personalizada
 * de la aplicación, usualmente utilizada después del inicio de sesión.
 *
 * @property personId Identificador del usuario o jugador, empleado para mostrar
 * información personalizada o cargar datos específicos del usuario.
 */
@Serializable
data class Home(val personId: String)
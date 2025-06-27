package com.example.taller2components.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taller2components.Enum.EnumNavigation
import com.example.taller2components.views.HomeScreen
import com.example.taller2components.views.LoginScreen
import com.example.taller2components.views.MainGame

/**
 * Navigation es una función composable responsable de definir la estructura de navegación
 * entre pantallas dentro de la aplicación del juego 4 en línea.
 *
 * Utiliza un NavHost para gestionar el flujo entre:
 * - Pantalla de Login (LOGIN)
 * - Pantalla de Inicio (HOME)
 * - Pantalla del Juego (PLAY), la cual requiere un parámetro `idBoard`
 *
 * La navegación está basada en los valores del enum [EnumNavigation], los cuales son convertidos
 * a cadena para definir las rutas.
 */
@Composable
fun Navigation() {
    // Controlador de navegación que permite movernos entre las diferentes pantallas
    val navController = rememberNavController()

    // Host de navegación que define las rutas y composables asociados
    NavHost(
        navController = navController,
        startDestination = EnumNavigation.LOGIN.toString()
    ) {

        // Ruta para la pantalla de inicio de sesión
        composable(EnumNavigation.LOGIN.toString()) {
            LoginScreen(navController)
        }

        // Ruta para la pantalla de inicio del usuario
        composable(EnumNavigation.HOME.toString()) {
            HomeScreen(navController)
        }

        // Ruta para la pantalla del juego, que espera un parámetro idBoard
        composable(
            route = "${EnumNavigation.PLAY}/{idBoard}",
            arguments = listOf(navArgument("idBoard") { type = NavType.StringType })
        ) {
            val idBoard = it.arguments?.getString("idBoard")
            MainGame(idBoard, navController)
        }
    }
}
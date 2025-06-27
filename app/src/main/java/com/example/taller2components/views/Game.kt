package com.example.taller2components.views


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.taller2components.Enum.EnumNavigation
import com.example.taller2components.ViewModel.GameViewModel
import com.example.taller2components.persistence.Casilla
import com.example.taller2components.persistence.Player
import com.example.taller2components.persistence.Tablero

/**
 * Composable principal que representa la pantalla del juego "4 en Raya".
 *
 * Se encarga de:
 * - Cargar y observar el estado del tablero desde el ViewModel.
 * - Mostrar una pantalla de carga o de espera si el juego aún no ha iniciado.
 * - Renderizar el componente del tablero si el juego ya ha comenzado.
 *
 * @param idBoard ID del tablero actual, pasado como argumento de navegación.
 * @param navController Controlador de navegación para redireccionar entre pantallas.
 * @param viewModel ViewModel del juego que maneja el estado de los datos y lógica.
 */

/**
 * Composable principal que representa la pantalla del juego "4 en Raya".
 *
 * Se encarga de:
 * - Cargar y observar el estado del tablero desde el ViewModel.
 * - Mostrar una pantalla de carga o de espera si el juego aún no ha iniciado.
 * - Renderizar el componente del tablero si el juego ya ha comenzado.
 *
 * @param idBoard ID del tablero actual, pasado como argumento de navegación.
 * @param navController Controlador de navegación para redireccionar entre pantallas.
 * @param viewModel ViewModel del juego que maneja el estado de los datos y lógica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGame(idBoard: String?, navController: NavHostController, viewModel: GameViewModel = viewModel()) {
    if (idBoard == null) {
        navController.navigate(EnumNavigation.LOGIN.toString())
        return
    }

    val id = remember { mutableStateOf(idBoard) }
    val board by viewModel.board.collectAsState()
    val players by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", 0)
    val currentUserId = prefs.getString("user_id", null)

    LaunchedEffect(id.value) {
        viewModel.listenToPlayers(id.value)
        viewModel.consultarTablero(id.value)
    }

    when {
        isLoading -> {
            // Pantalla de carga mientras se consulta el tablero
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Cargando datos del tablero...")
            }
        }

        board?.state == true -> {
            val tablero = board?.let { Tablero.iniciarTablero(it) }

            // Pantalla del juego
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("4 en Raya") }
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    currentUserId?.let {
                        TableroScreen(
                            tablero = tablero,
                            players = players,
                            currentUserId = it,
                            currentTurn = board!!.currentPlayerIndex,
                            onColumnSelected = { col, player ->
                                viewModel.switchTurn(id.value)
                                viewModel.makeMove(id.value, col, player)
                            }
                        )
                    }
                }
            }
        }

        else -> {
            // Mensaje mientras se espera al host
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Esperando a que el host inicie el juego...")
            }
        }
    }
}

/**
 * Composable que representa visualmente el tablero de juego con las fichas y
 * controles para que el jugador actual seleccione su movimiento.
 *
 * @param tablero Matriz bidimensional de casillas del tablero.
 * @param players Lista de jugadores en la partida.
 * @param currentUserId ID del jugador actual (identificado desde SharedPreferences).
 * @param currentTurn Número de turno (1 o 2) que indica a qué jugador le corresponde jugar.
 * @param onColumnSelected Función callback que se invoca cuando se selecciona una columna para jugar.
 */
@Composable
fun TableroScreen(
    tablero: List<List<Casilla>>?,
    players: List<Player>,
    currentUserId: String,
    currentTurn: Int,
    onColumnSelected: (Int, Player) -> Unit
) {
    if (tablero.isNullOrEmpty() || tablero[0].isEmpty()) return

    val playerTurn = players.find { it.turno == currentTurn }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mostrar de quién es el turno
        Text(
            text = "Turno de: ${playerTurn?.correo}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Mostrar tablero visual
        Box(modifier = Modifier.padding(4.dp)) {
            Column {
                for (row in tablero.indices) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (col in tablero[row].indices) {
                            val casilla = tablero[row][col]
                            val player = players.find { it.turno == casilla.valor }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(2.dp)
                                    .background(Color.White.copy(alpha = 0.8f))
                                    .border(1.dp, Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (casilla.valor != 0 && player != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = player.color.colorFromHex(),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Flechas de selección para el jugador en turno
        if (playerTurn != null && playerTurn.idPlayer == currentUserId) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in tablero[0].indices) {
                    val isColumnFull = tablero.all { row -> row[col].valor != 0 }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                            .clickable(enabled = !isColumnFull) {
                                onColumnSelected(col, playerTurn)
                            }
                            .background(
                                if (isColumnFull) Color.Red.copy(alpha = 0.3f)
                                else Color.LightGray.copy(alpha = 0.5f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isColumnFull) Icons.Default.Close
                            else Icons.Default.ArrowDropDown,
                            contentDescription = "Columna $col",
                            tint = if (isColumnFull) Color.Red else Color.Blue
                        )
                    }
                }
            }
        }

        // Indicador de jugadores
        playerTurn?.let { PlayersIndicator(players, it) }
    }
}

/**
 * Muestra una lista visual de los jugadores en partida con sus colores e identifica
 * visualmente quién tiene el turno actual.
 *
 * @param players Lista completa de jugadores.
 * @param currentPlayer Jugador al que le corresponde el turno.
 */

@Composable
private fun PlayersIndicator(players: List<Player>, currentPlayer: Player) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Jugadores:", style = MaterialTheme.typography.titleMedium)

        players.forEach { player ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(player.color.colorFromHex(), CircleShape)
                        .border(1.dp, Color.Black, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = player.correo,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (player.idPlayer == currentPlayer.idPlayer) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }
}

/**
 * Funcion para convertir un color hexadecimal (ej: "#FF0000") en un objeto [Color].
 *
 * @receiver String con formato hexadecimal.
 * @return Color correspondiente o [Color.Black] si el formato es inválido.
 */
fun String.colorFromHex(): Color {
    return try {
        Color(this.toColorInt())
    } catch (e: Exception) {
        Color.Black
    }
}
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.example.taller2components.persistence.Pregunta
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
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", 0)
    val currentUserId = prefs.getString("user_id", null)
    val mostrarPregunta = remember { mutableStateOf(false) }
    val preguntaActual = remember { mutableStateOf<Pregunta?>(null) }
    val preguntaRespondida = remember { mutableStateOf(false) }
    LaunchedEffect(id.value) {
        id.value.let { viewModel.consultarTablero(it) }
    }

    when {
        isLoading -> {
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
            LaunchedEffect(board!!.currentPlayerIndex) {
                Log.d("MainGame", "prueba")
                val playerTurn = board!!.players.find { it.turno == board!!.currentPlayerIndex }
                Log.d("MainGame", "prueba2: "+playerTurn?.toString()+ currentUserId)
                // Validamos si el turno actual le pertenece al usuario conectado
                if (playerTurn != null && playerTurn.idPlayer == currentUserId) {
                    // Reiniciar estado de pregunta cuando cambia el turno del jugador actual
                    preguntaRespondida.value = false

                    if (!preguntaRespondida.value) {
                        val random = (1..4).random()
                        viewModel.consultarPregunta(random.toString()) { pregunta ->
                            if (pregunta != null) {
                                Log.d("MainGame", "prueba3: "+(pregunta))

                                preguntaActual.value = pregunta
                                mostrarPregunta.value = true
                            } else {
                                Log.w("MainGame", "No se encontró pregunta con ID: $random")
                            }
                        }
                    }
                }
            }


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
                            grilla = board!!.grid,
                            players = board!!.players,
                            currentUserId = it,
                            currentTurn = board!!.currentPlayerIndex,
                            onColumnSelected = { col, player ->
                                viewModel.switchTurn(id.value)
                                viewModel.makeMove(id.value, col,  player)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (mostrarPregunta.value && preguntaActual.value != null) {
                        val pregunta = preguntaActual.value!!
                        var selectedIndex by remember { mutableStateOf(-1) }

                        AlertDialog(
                            onDismissRequest = { /* no se puede cerrar */ },
                            title = { Text("Pregunta para continuar") },
                            text = {
                                Column {
                                    Text(pregunta.pregunta)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    pregunta.opciones.forEachIndexed { index, opcion ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedIndex = index }
                                                .padding(4.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedIndex == index,
                                                onClick = { selectedIndex = index }
                                            )
                                            Text(text = opcion)
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (selectedIndex == pregunta.respuesta) {
                                            // Respuesta correcta
                                            mostrarPregunta.value = false
                                            preguntaRespondida.value = true
                                        } else {
                                            // Respuesta incorrecta, cambia turno
                                            viewModel.switchTurn(id.value)
                                            mostrarPregunta.value = false
                                            preguntaRespondida.value = true
                                        }
                                    },
                                    enabled = selectedIndex != -1
                                ) {
                                    Text("Responder")
                                }
                            }
                        )
                    }

                }
            }
        }
        else -> {
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

@Composable
fun TableroScreen(
    grilla: List<Casilla>,
    players: List<Player>,
    currentUserId: String,
    currentTurn: Int,
    onColumnSelected: (Int, Player) -> Unit
) {
    if (grilla.isEmpty()) return

    // Determinar dimensiones
    val filas = grilla.maxOfOrNull { it.fila }?.plus(1) ?: 6
    val columnas = grilla.maxOfOrNull { it.columna }?.plus(1) ?: 7

    // Reconstruir la matriz a partir de la lista plana
    val tablero = List(filas) { fila ->
        List(columnas) { columna ->
            grilla.find { it.fila == fila && it.columna == columna } ?: Casilla(fila, columna, 0, "")
        }
    }

    val playerTurn = players.find { it.turno == currentTurn }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Información de turno
        Text(
            text = "Turno de: ${playerTurn?.correo ?: "Desconocido"}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Botones de selección por columna
        if (playerTurn != null && playerTurn.idPlayer == currentUserId) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 0 until columnas) {
                    val isColumnFull = tablero.all { row -> row[col].valor != 0 }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(4.dp)
                            .clickable(
                                enabled = !isColumnFull,
                                onClick = { onColumnSelected(col, playerTurn) }
                            )
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

        // Render de la grilla
        Column {
            for (fila in tablero.indices) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (col in tablero[fila].indices) {
                        val casilla = tablero[fila][col]
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

        // Indicador de jugadores
        playerTurn?.let { PlayersIndicator(players, it) }
    }
}


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

// Función de extensión para convertir String a Color
fun String.colorFromHex(): Color {
    return try {
        Color(this.toColorInt())
    } catch (e: Exception) {
        Color.Black // Color por defecto en caso de error
    }
}
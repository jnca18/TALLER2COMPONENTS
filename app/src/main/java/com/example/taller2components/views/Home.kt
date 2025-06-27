package com.example.taller2components.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taller2components.Enum.EnumNavigation
import com.example.taller2components.R
import com.example.taller2components.ViewModel.GameBoardViewModel
import com.example.taller2components.persistence.Operaciones
import com.example.taller2components.persistence.Player
import com.google.firebase.auth.FirebaseAuth


/**
 * Modelo de datos que representa una partida anterior ya finalizada.
 *
 * @property id Identificador único de la partida.
 * @property date Fecha en la que se jugó la partida.
 * @property winner Correo del jugador que ganó la partida.
 * @property score Puntuación obtenida por el ganador u otra métrica.
 * @property players Lista de correos de los jugadores que participaron.
 */
data class PastGame(
    val id: String,
    val date: String,
    val winner: String,
    val score: String,
    val players: List<String>
)

@Preview
@Composable
fun PreviewHomeScreen() {
    // Preview placeholder
}

/**
 * Composable que representa la pantalla de inicio del juego "Four In Row".
 *
 * Permite al usuario:
 * - Crear una nueva partida multijugador.
 * - Unirse a una partida existente mediante un código.
 * - Ver un historial de partidas anteriores (ejemplo simulado).
 *
 * @param navController Controlador de navegación para cambiar de pantalla.
 * @param viewModel ViewModel que gestiona la creación y unión a tableros de juego.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: GameBoardViewModel = viewModel()) {
    var host by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var showMultiplayerDialog by remember { mutableStateOf(false) }
    var showNewGameDialog by remember { mutableStateOf(false) }

    val players by viewModel.players.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Lista simulada de partidas anteriores (se debe reemplazar con datos reales)
    val pastGames = remember {
        listOf(
            PastGame("001", "2023-05-15", "jugador1@example.com", "120 puntos", listOf("jugador1@example.com", "jugador2@example.com")),
            PastGame("002", "2023-05-10", "jugador3@example.com", "95 puntos", listOf("jugador3@example.com", "jugador4@example.com")),
            PastGame("003", "2023-05-05", "jugador2@example.com", "110 puntos", listOf("jugador1@example.com", "jugador2@example.com"))
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Row {
                    Text("Four", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    Spacer(Modifier.width(5.dp))
                    Text("In", fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Spacer(Modifier.width(5.dp))
                    Text("Row", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                }
            })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Botón para crear o unirse a partida
            Button(
                onClick = { showMultiplayerDialog = true },
                modifier = Modifier
                    .padding(10.dp)
                    .width(200.dp)
            ) {
                Text(text = "Nueva Partida", fontSize = 18.sp)
            }

            Spacer(Modifier.height(40.dp))

            // Sección de historial
            Text("Partidas Anteriores", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(10.dp))
            PastGamesList(pastGames = pastGames)

            // Diálogo para crear o unirse a una partida
            if (showMultiplayerDialog) {
                AlertDialog(
                    onDismissRequest = { showMultiplayerDialog = false },
                    title = { Text("Multijugador") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it },
                                label = { Text("Código de la partida") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))

                            if (host.isNotEmpty()) {
                                val context = LocalContext.current
                                Text("O crea una nueva partida:", fontWeight = FontWeight.Bold)
                                Card(
                                    modifier = Modifier.clickable {
                                        copyToClipboard(context, host)
                                        Toast.makeText(context, "ID de partida copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(9.dp),
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Row(
                                        Modifier.padding(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(host, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            painter = painterResource(R.drawable.copy_icon),
                                            contentDescription = null,
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (joinCode.isNotEmpty()) {
                                    val user = FirebaseAuth.getInstance().currentUser
                                    if (user != null) {
                                        val player = Player(
                                            idPlayer = user.uid,
                                            correo = user.email ?: "",
                                            Operaciones().generateRandomColor(),
                                            0,
                                            isCurrentTurn = true
                                        )
                                        viewModel.joinBoard(joinCode, player)
                                        navController.navigate("${EnumNavigation.PLAY}/$joinCode")
                                        showMultiplayerDialog = false
                                    } else {
                                        navController.navigate(EnumNavigation.LOGIN.toString())
                                    }
                                } else if (host.isEmpty()) {
                                    host = viewModel.createGameBoard()
                                }
                            }
                        ) {
                            Text(if (joinCode.isNotEmpty()) "Unirse" else "Crear Partida")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showMultiplayerDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Composable que representa una lista vertical de partidas anteriores.
 *
 * @param pastGames Lista de partidas ya jugadas.
 */
@Composable
fun PastGamesList(pastGames: List<PastGame>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        items(pastGames) { game ->
            PastGameItem(game)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Composable que representa una tarjeta individual de partida anterior.
 *
 * @param game Datos de la partida a mostrar.
 */
@Composable
fun PastGameItem(game: PastGame) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Acción futura: ver detalles de la partida */ },
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Partida: ${game.id}", fontWeight = FontWeight.Bold)
                Text(game.date, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Ganador: ${game.winner}", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Puntuación: ${game.score}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Jugadores: ${game.players.joinToString(", ")}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}


/**
 * Copia el texto especificado al portapapeles del dispositivo.
 *
 * @param context Contexto actual (necesario para obtener ClipboardManager).
 * @param text Texto que se desea copiar.
 */
fun copyToClipboard(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("ID Partida", text)
    clipboardManager.setPrimaryClip(clipData)
}
package ud.example.four_in_row.views


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ud.example.four_in_row.Enum.EnumNavigation
import ud.example.four_in_row.ViewModel.GameViewModel
import ud.example.four_in_row.persistence.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGame(idBoard: String?, navController: NavHostController, viewModel: GameViewModel = viewModel()) {
    if (idBoard == null) {
        navController.navigate(EnumNavigation.LOGIN.toString())
        return
    }

    val id = remember { mutableStateOf(idBoard) }
    val players by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentUserEmail = viewModel.currentUserEmail
    val currentPlayerTurn by viewModel.currentPlayerTurn.collectAsState()
    val grid by viewModel.gameGrid.collectAsState()
    val gameStatus by viewModel.gameStatus.collectAsState()

    LaunchedEffect(id.value) {
        viewModel.listenToGame(id.value) // Escuchar cambios en el juego
    }

    when {
        isLoading -> LoadingScreen()
        gameStatus == "finished" -> GameFinishedScreen(players, navController)
        players.size < 2 -> WaitingForPlayersScreen()
        else -> {
            Scaffold(
                topBar = { GameTopBar(gameStatus) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding).fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CurrentPlayerTurn(currentPlayerTurn, currentUserEmail)
                    Spacer(modifier = Modifier.height(20.dp))
                    FourInRowGrid(
                        grid = grid,
                        onColumnClick = { column ->
                            if (currentPlayerTurn?.correo == currentUserEmail && gameStatus == "playing") {
                                viewModel.makeMove(id.value, column)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PlayersInfo(players, currentPlayerTurn)
                }
            }
        }
    }

    ErrorMessage(errorMessage)
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text("Cargando datos del tablero...")
    }
}

@Composable
private fun GameFinishedScreen(players: List<Player>, navController: NavHostController) {
    val winner = players.firstOrNull { it.isWinner }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (winner != null) "¡${winner.correo} ha ganado!" else "¡Empate!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text("Volver al menú principal")
        }
    }
}

@Composable
private fun WaitingForPlayersScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text("Esperando al segundo jugador...")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(gameStatus: String) {
    CenterAlignedTopAppBar(
        title = {
            Row {
                Text("4 en Raya")
                if (gameStatus == "finished") {
                    Text(" - Terminado", color = Color.Red)
                }
            }
        }
    )
}

@Composable
private fun CurrentPlayerTurn(currentPlayer: Player?, currentUserEmail: String) {
    currentPlayer?.let { player ->
        Text(
            text = "Turno de: ${player.correo}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = if (player.correo == currentUserEmail) Color.Green else Color.Black
        )
    }
}

@Composable
private fun ErrorMessage(errorMessage: String?) {
    errorMessage?.let {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = it, color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}
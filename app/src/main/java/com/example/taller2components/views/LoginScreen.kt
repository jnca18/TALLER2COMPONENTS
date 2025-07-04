package com.example.taller2components.views

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taller2components.Enum.EnumNavigation

/**
 * Vista de previsualización para la pantalla de login utilizando Navigation Compose.
 * Inicia la app en la pantalla "Login".
 */
@Preview
@Composable
fun ViewLoginScreem(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "Login"){
        composable("Login") {
            LoginScreen(navController)
        }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }
    var isLoading by remember { mutableStateOf(false) }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_prefs", 0)

    Scaffold{ innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    "Four",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "In",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Row",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Email TextField
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.width(300.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password TextField
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.width(300.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button
            Button(
                onClick = {
                    isLoading = true
                    auth.signInWithEmailAndPassword(email.text, password.text)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                //Almaceno el id de la persona logeada e ingresada
                                val userId = task.result?.user?.uid ?: ""
                                prefs.edit { putString("user_id", userId) }
                                // Mostrar Toast usando contexto
                                Toast.makeText(context, "¡Login exitoso!", Toast.LENGTH_SHORT).show()
                                navController.navigate(EnumNavigation.HOME.toString())
                            } else {
                                // Mostrar Toast con error
                                Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                enabled = email.text.isNotBlank() && password.text.isNotBlank() && !isLoading,
                modifier = Modifier.width(300.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Login")
                }
            }
        }
    }
}

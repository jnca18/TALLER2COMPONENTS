package com.example.taller2components

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.Navigation
import com.example.taller2components.ui.theme.Taller2componentsTheme
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Conexión con Firebase Realtime Database
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("mensaje_prueba")

        myRef.setValue("¡Hola desde Firebase!")
            .addOnSuccessListener {
                Toast.makeText(this, "Conexión exitosa", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }

        setContent {
            Taller2componentsTheme {
                Navigation
            }
        }
    }
}


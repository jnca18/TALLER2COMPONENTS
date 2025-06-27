package com.example.taller2components.navigation

import kotlinx.serialization.Serializable


@Serializable
object Login

@Serializable
data class Game(val gameId: String)

@Serializable
data class Home(val personId: String)

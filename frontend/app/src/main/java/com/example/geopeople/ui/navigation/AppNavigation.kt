package com.example.geopeople.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geopeople.ui.inventory.InventoryScreen
import com.example.geopeople.ui.map.GameScreen
import com.example.geopeople.viewmodel.GameViewModel

@Composable
fun AppNavigation(viewModel: GameViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "map",
                    onClick = {
                        navController.navigate("map") {
                            popUpTo("map") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Place, contentDescription = "Carte") },
                    label = { Text("Carte") }
                )
                NavigationBarItem(
                    selected = currentRoute == "inventory",
                    onClick = {
                        navController.navigate("inventory") {
                            popUpTo("map")
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Inventaire") },
                    label = { Text("Inventaire") }
                )
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = "map", Modifier.padding(padding)) {
            composable("map") { GameScreen(viewModel) }
            composable("inventory") {
                val inventory by viewModel.inventory.collectAsState()
                InventoryScreen(inventory)
            }
        }
    }
}

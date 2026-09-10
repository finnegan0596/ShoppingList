package com.finnegan0596.shoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.finnegan0596.shoppinglist.ui.ShoppingListViewModel
import com.finnegan0596.shoppinglist.ui.screens.SettingsScreen
import com.finnegan0596.shoppinglist.ui.screens.ShopsScreen
import com.finnegan0596.shoppinglist.ui.screens.ShoppingListScreen
import com.finnegan0596.shoppinglist.ui.theme.ShoppingListTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShoppingListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppingListTheme {
                ShoppingListApp(viewModel)
            }
        }
    }
}

private sealed class Destination(val route: String, val label: String) {
    data object List : Destination("list", "List")
    data object Shops : Destination("shops", "Shops")
    data object Settings : Destination("settings", "Data")
}

@Composable
fun ShoppingListApp(viewModel: ShoppingListViewModel) {
    val navController = rememberNavController()
    val destinations = listOf(Destination.List, Destination.Shops, Destination.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (destination) {
                                Destination.List -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = destination.label)
                                Destination.Shops -> Icon(Icons.Filled.Storefront, contentDescription = destination.label)
                                Destination.Settings -> Icon(Icons.Filled.Settings, contentDescription = destination.label)
                            }
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.List.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.List.route) { ShoppingListScreen(viewModel) }
            composable(Destination.Shops.route) { ShopsScreen(viewModel) }
            composable(Destination.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}

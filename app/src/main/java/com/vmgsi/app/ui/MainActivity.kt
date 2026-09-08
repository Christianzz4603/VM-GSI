package com.vmgsi.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VmGsiApp()
        }
    }
}

@Composable
fun VmGsiApp() {
    MaterialTheme {
        Surface {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "catalog") {
                composable("catalog") { GsiCatalogScreen(navController) }
                composable("library") { LibraryScreen(navController) }
                composable("vm/{imageId}") { backStackEntry ->
                    val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
                    VmScreen(imageId)
                }
            }
        }
    }
}

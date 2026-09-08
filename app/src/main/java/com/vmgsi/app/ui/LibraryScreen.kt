package com.vmgsi.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Shows images that are downloaded and ready to boot — plain or
 * Magisk-patched — with acceleration mode (KVM vs software) surfaced per
 * image based on the current host device's root/KVM status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My VMs") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("No images downloaded yet — pick one from Android versions.")
        }
    }
}

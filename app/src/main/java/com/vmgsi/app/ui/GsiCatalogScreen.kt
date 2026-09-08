package com.vmgsi.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vmgsi.app.R
import com.vmgsi.app.data.GsiRelease
import com.vmgsi.app.gsi.GsiCatalog
import com.vmgsi.app.util.ByteFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GsiCatalogScreen(navController: NavController) {
    val releases = remember { GsiCatalog.offlineFallbackCatalog() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Android versions") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(releases) { release ->
                GsiReleaseCard(release)
            }
        }
    }
}

@Composable
private fun GsiReleaseCard(release: GsiRelease) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Android ${release.androidVersion}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${release.abi} · ${release.buildVariant} · ${ByteFormatter.humanReadable(release.sizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { /* TODO: enqueue GsiDownloader work */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Download")
                }
                OutlinedButton(onClick = { /* TODO: download + queue Magisk patch */ }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_root_shield),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Download rooted")
                }
            }
        }
    }
}

package com.vmgsi.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vmgsi.app.R

/**
 * Hosts the VNC client view connected to the running QEMU instance's VNC
 * server (localhost, port from VmConfig.vncPort). The actual VNC rendering
 * surface reuses the viewer component from the Proot Distro app rather than
 * being reimplemented here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VmScreen(imageId: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booting…") },
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_vm_boot),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp).size(20.dp),
                    )
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            // TODO: embed shared VNC viewer component here, pointed at
            // localhost:<vncPort> once QemuVmService reports it's up
            Text("VNC viewer goes here for image: $imageId")
        }
    }
}

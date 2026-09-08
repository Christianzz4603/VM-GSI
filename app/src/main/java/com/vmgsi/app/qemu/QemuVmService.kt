package com.vmgsi.app.qemu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Keeps the QEMU process alive as a foreground service so the VM keeps
 * running while the user navigates away from the app (e.g. to use the VNC
 * viewer full-screen, or switch apps briefly).
 */
class QemuVmService : Service() {

    private var launcher: QemuLauncher? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        // VmConfig would be passed in via the intent extras in a full
        // implementation, then handed to QemuLauncher.start()
        return START_STICKY
    }

    override fun onDestroy() {
        launcher?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "vm_gsi_running"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VM running",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Android VM running")
            .setContentText("Tap to return to the VM screen")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

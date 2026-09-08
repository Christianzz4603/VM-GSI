package com.vmgsi.app.qemu

import java.io.File

/**
 * Detects whether the *host* device is rooted and whether /dev/kvm is
 * actually accessible — this determines KVM-accelerated vs software (TCG)
 * emulation. This app never attempts to root the device itself; it only
 * reacts to root that's already present (e.g. via Magisk).
 */
object RootDetector {

    fun isHostRooted(): Boolean {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
        )
        if (suPaths.any { File(it).exists() }) return true

        return try {
            val process = ProcessBuilder("which", "su").start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /** KVM needs both root (to access the device node) and kernel support. */
    fun isKvmAvailable(): Boolean {
        val kvmNode = File("/dev/kvm")
        return isHostRooted() && kvmNode.exists()
    }

    enum class AccelerationMode { KVM, SOFTWARE_TCG }

    fun recommendedMode(): AccelerationMode =
        if (isKvmAvailable()) AccelerationMode.KVM else AccelerationMode.SOFTWARE_TCG
}

package com.vmgsi.app.qemu

import com.vmgsi.app.data.Abi
import java.io.File

data class VmConfig(
    val systemImage: File,
    val abi: Abi,
    val ramMb: Int = 2048,
    val cpuCores: Int = 2,
    val vncPort: Int = 5901,
    val accelerationMode: RootDetector.AccelerationMode = RootDetector.recommendedMode(),
)

/**
 * Wraps a prebuilt QEMU binary (bundled as a native executable under
 * jniLibs, one per ABI) and boots the given system image, exposing a VNC
 * server on localhost for the in-app VNC viewer to connect to.
 *
 * QEMU itself is NOT built by this app — it's compiled separately (from
 * upstream QEMU source, targeting Android via the NDK) and the resulting
 * binaries are bundled as native libraries. That build is its own toolchain
 * setup, tracked separately from this app's Kotlin/UI code.
 */
class QemuLauncher(private val nativeLibDir: File) {

    private var process: Process? = null

    fun start(config: VmConfig): Result<Unit> {
        val qemuBinary = File(nativeLibDir, qemuBinaryNameFor(config.abi))
        if (!qemuBinary.exists()) {
            return Result.failure(
                IllegalStateException(
                    "QEMU binary not found for ${config.abi} — was it bundled in this build?"
                )
            )
        }

        val args = buildArgs(config, qemuBinary)

        return try {
            process = ProcessBuilder(args)
                .redirectErrorStream(true)
                .start()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true

    private fun qemuBinaryNameFor(abi: Abi): String = when (abi) {
        Abi.ARM64 -> "libqemu-system-aarch64.so"   // .so extension required for
        Abi.X86_64 -> "libqemu-system-x86_64.so"   // Android to package executables in jniLibs
    }

    private fun buildArgs(config: VmConfig, qemuBinary: File): List<String> {
        val args = mutableListOf(
            qemuBinary.absolutePath,
            "-m", config.ramMb.toString(),
            "-smp", config.cpuCores.toString(),
            "-drive", "file=${config.systemImage.absolutePath},format=raw,if=virtio",
            "-vnc", ":${config.vncPort - 5900}",
            "-display", "none",
        )

        when (config.accelerationMode) {
            RootDetector.AccelerationMode.KVM -> {
                args += listOf("-enable-kvm", "-cpu", "host")
            }
            RootDetector.AccelerationMode.SOFTWARE_TCG -> {
                // Software emulation — functional but significantly slower.
                // Surfaced to the user in the UI so expectations are set.
                args += listOf("-accel", "tcg")
            }
        }

        return args
    }
}

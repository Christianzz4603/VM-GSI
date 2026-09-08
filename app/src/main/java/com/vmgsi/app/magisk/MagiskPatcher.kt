package com.vmgsi.app.magisk

import java.io.File

sealed class PatchResult {
    data class Success(val patchedImage: File) : PatchResult()
    data class Failed(val reason: String) : PatchResult()
}

/**
 * Pre-boot patching step: takes a downloaded GSI (must be a `userdebug`
 * build — `user` builds are locked down and not supported), extracts its
 * boot image, patches it with Magisk, and repacks it into a bootable image
 * ready for QEMU.
 *
 * Magisk's patcher normally runs *on* the target device and inspects the
 * live boot partition. Here we run the same patch logic directly against
 * the raw GSI boot image file instead — we control both ends of the
 * pipeline (the file and the "device" is the VM), so no physical phone or
 * root on the host is required for this step. The host device's own root
 * status is unrelated to this and only affects KVM acceleration.
 *
 * Requires bundling Magisk's standalone patching assets (magiskboot binary
 * + magisk.apk data files) with the app — these come from Magisk's official
 * GitHub releases, not a third-party source.
 */
class MagiskPatcher(
    private val workingDir: File,
    private val magiskBootBinary: File,
) {

    fun patch(rawSystemImage: File, outputDir: File): PatchResult {
        if (!magiskBootBinary.exists()) {
            return PatchResult.Failed(
                "magiskboot binary not found — bundled Magisk assets are missing"
            )
        }

        return try {
            val bootImage = extractBootImage(rawSystemImage)
                ?: return PatchResult.Failed("Could not locate boot.img inside the GSI package")

            val patchedBoot = runMagiskBootPatch(bootImage)
                ?: return PatchResult.Failed("magiskboot patch step failed")

            val finalImage = repackImage(rawSystemImage, patchedBoot, outputDir)
            PatchResult.Success(finalImage)
        } catch (e: Exception) {
            PatchResult.Failed(e.message ?: "Unknown error during Magisk patching")
        }
    }

    private fun extractBootImage(systemImage: File): File? {
        // Boot image location within the GSI package itself (zip layout,
        // sparse image handling) still needs to be resolved — GSI packages
        // vary in whether boot.img ships separately or needs generating.
        // What IS implemented: once we have raw boot.img bytes, the Rust
        // parser (NativeBootImage) locates the ramdisk range inside it so we
        // know exactly what to hand to magiskboot.
        TODO(
            "Locate/extract the raw boot.img from the GSI package's zip or " +
            "sparse image layout, then read its bytes and call " +
            "NativeBootImage.ramdiskRange(bytes) to find the ramdisk slice."
        )
    }

    private fun runMagiskBootPatch(bootImage: File): File? {
        TODO(
            "Shell out to magiskboot: unpack ramdisk, inject Magisk's magisk " +
            "binary + init patch, repack ramdisk, matching the steps Magisk's " +
            "own Shell.sh install script performs on-device."
        )
    }

    private fun repackImage(original: File, patchedBoot: File, outputDir: File): File {
        TODO("Reassemble the system image with the patched boot image swapped in.")
    }
}

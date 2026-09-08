package com.vmgsi.app.data

/**
 * Represents one downloadable GSI (Generic System Image) build.
 *
 * Sourced from Google's official AOSP GSI release pages — never third-party
 * mirrors or unverified channels. See:
 * https://developer.android.com/topic/generic-system-image/releases
 */
data class GsiRelease(
    val androidVersion: String,      // e.g. "12", "11", "10"
    val apiLevel: Int,               // e.g. 31
    val buildVariant: BuildVariant,
    val abi: Abi,
    val downloadUrl: String,
    val sha256: String,              // required — verified before the image is trusted
    val sizeBytes: Long,
)

enum class BuildVariant {
    /** Locked down, no adb root, cannot be Magisk-patched via this app. */
    USER,

    /** adb root available; required for the in-app Magisk pre-patch step. */
    USERDEBUG,
}

enum class Abi {
    ARM64,
    X86_64,
}

/** Local, on-device record of an image the user has downloaded and/or patched. */
data class LocalImage(
    val id: String,
    val release: GsiRelease,
    val filePath: String,
    val state: ImageState,
    val rooted: Boolean,             // true if this copy was Magisk-patched
)

enum class ImageState {
    DOWNLOADING,
    VERIFYING,
    READY,               // downloaded + checksum verified, unpatched
    PATCHING,
    READY_ROOTED,        // Magisk-patched and ready to boot
    FAILED,
}

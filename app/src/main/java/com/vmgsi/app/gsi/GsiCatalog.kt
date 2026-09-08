package com.vmgsi.app.gsi

import com.vmgsi.app.data.Abi
import com.vmgsi.app.data.BuildVariant
import com.vmgsi.app.data.GsiRelease

/**
 * Known-good GSI releases this app can offer, scoped to Android 12 and below
 * per the current project target.
 *
 * NOTE: URLs and checksums below are placeholders. Google's real download
 * URLs/hashes for each dessert release must be filled in from the official
 * page before this ships — see fetchLiveManifest() for the intended
 * long-term source of truth instead of hardcoding.
 */
object GsiCatalog {

    val supportedAndroidVersions = listOf("12", "11", "10", "9", "8.1", "8.0")

    fun offlineFallbackCatalog(): List<GsiRelease> = listOf(
        GsiRelease(
            androidVersion = "12",
            apiLevel = 31,
            buildVariant = BuildVariant.USERDEBUG,
            abi = Abi.ARM64,
            downloadUrl = "https://dl.google.com/dl/android/aosp/gsi/PLACEHOLDER-arm64-ab-userdebug.zip",
            sha256 = "PLACEHOLDER",
            sizeBytes = 0L,
        ),
        GsiRelease(
            androidVersion = "12",
            apiLevel = 31,
            buildVariant = BuildVariant.USERDEBUG,
            abi = Abi.X86_64,
            downloadUrl = "https://dl.google.com/dl/android/aosp/gsi/PLACEHOLDER-x86_64-ab-userdebug.zip",
            sha256 = "PLACEHOLDER",
            sizeBytes = 0L,
        ),
        // Older versions (11 down to 8.0) follow the same pattern and get
        // filled in the same way once real manifest data is wired up.
    )

    /**
     * Long-term source of truth: Google publishes an index of GSI builds.
     * This should fetch and parse that index at runtime instead of relying
     * on the hardcoded fallback above, so new releases show up without an
     * app update.
     */
    suspend fun fetchLiveManifest(): List<GsiRelease> {
        TODO(
            "Fetch and parse the official GSI release index. " +
            "Falls back to offlineFallbackCatalog() on network failure."
        )
    }
}

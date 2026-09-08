package com.vmgsi.app.magisk

/**
 * Bridge to the Rust boot.img header parser (app/src/main/rust). Used to
 * locate the ramdisk's byte range inside a boot image before handing that
 * slice off to magiskboot for patching — parsing the untrusted binary
 * header is done in Rust specifically to avoid manual C pointer arithmetic
 * over attacker-influenceable/malformed-file input.
 */
object NativeBootImage {
    init {
        System.loadLibrary("vmgsi_bootimg")
    }

    /**
     * Returns the ramdisk's (offset, length) within [fileBytes], or null if
     * the file isn't a recognizable boot.img (bad magic, too short, etc).
     */
    fun ramdiskRange(fileBytes: ByteArray): Pair<Long, Long>? {
        val raw = parseRamdiskRange(fileBytes)
        if (raw.isEmpty()) return null
        val parts = raw.split(",")
        if (parts.size != 2) return null
        return parts[0].toLongOrNull()?.let { offset ->
            parts[1].toLongOrNull()?.let { length -> offset to length }
        }
    }

    private external fun parseRamdiskRange(fileBytes: ByteArray): String
}

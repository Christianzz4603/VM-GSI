package com.vmgsi.app.util

/**
 * Thin Kotlin wrapper over the native (C/C++) SHA-256 implementation in
 * app/src/main/cpp. Used instead of java.security.MessageDigest when
 * verifying large (multi-GB) GSI downloads — noticeably faster and avoids
 * extra JVM heap churn on a large file.
 */
object NativeChecksum {
    init {
        System.loadLibrary("vmgsi_checksum")
    }

    /** Returns the lowercase hex SHA-256 digest of the file at [filePath], or "" on error. */
    external fun sha256OfFile(filePath: String): String
}

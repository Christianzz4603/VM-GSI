package com.vmgsi.app.gsi

import com.vmgsi.app.data.GsiRelease
import com.vmgsi.app.util.NativeChecksum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

sealed class DownloadProgress {
    data class InProgress(val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress()
    data class Verifying(val fraction: Float) : DownloadProgress()
    data class Complete(val file: File) : DownloadProgress()
    data class Failed(val reason: String) : DownloadProgress()
}

/**
 * Downloads a GSI image with resume support and mandatory checksum
 * verification — an image is never marked READY unless its SHA-256 matches
 * what Google published.
 */
class GsiDownloader(
    private val client: OkHttpClient,
    private val destinationDir: File,
) {
    fun download(release: GsiRelease): Flow<DownloadProgress> = flow {
        val outFile = File(destinationDir, fileNameFor(release))
        val resumeOffset = if (outFile.exists()) outFile.length() else 0L

        val request = Request.Builder()
            .url(release.downloadUrl)
            .apply {
                if (resumeOffset > 0) {
                    header("Range", "bytes=$resumeOffset-")
                }
            }
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(DownloadProgress.Failed("HTTP ${response.code} from download server"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadProgress.Failed("Empty response body"))
                return@flow
            }

            val totalBytes = resumeOffset + body.contentLength()
            // append=true when resuming, so a partial download isn't
            // overwritten from byte 0 — actually honors the Range request above
            val sink = java.io.FileOutputStream(outFile, resumeOffset > 0).buffered(256 * 1024)
            val source = body.byteStream().buffered(256 * 1024)
            val buffer = ByteArray(256 * 1024)
            var downloaded = resumeOffset
            var lastEmit = 0L

            sink.use { out ->
                var read: Int
                while (source.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                    downloaded += read
                    // Throttle progress emissions — emitting every chunk on a
                    // fast connection floods the UI with redundant updates
                    if (downloaded - lastEmit > 512 * 1024 || read == -1) {
                        emit(DownloadProgress.InProgress(downloaded, totalBytes))
                        lastEmit = downloaded
                    }
                }
            }
        }

        emit(DownloadProgress.Verifying(0f))

        // Native (C/C++) hashing — meaningfully faster than the JVM loop
        // below on multi-GB images. Falls back to the pure-Kotlin
        // implementation if the native library failed to load for any
        // reason (e.g. unsupported ABI), so verification still works.
        val nativeHash = NativeChecksum.sha256OfFile(outFile.absolutePath)
        val actualHash = nativeHash.ifEmpty {
            sha256Of(outFile) { /* fraction callback unused in fallback path */ }
        }

        if (!actualHash.equals(release.sha256, ignoreCase = true)) {
            outFile.delete()
            emit(
                DownloadProgress.Failed(
                    "Checksum mismatch — refusing to use this file. " +
                    "Expected ${release.sha256}, got $actualHash"
                )
            )
            return@flow
        }

        emit(DownloadProgress.Complete(outFile))
    }

    private fun fileNameFor(release: GsiRelease): String =
        "gsi-android${release.androidVersion}-${release.abi}-${release.buildVariant}.zip"

    private fun sha256Of(file: File, onProgress: (Float) -> Unit): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val total = file.length().coerceAtLeast(1)
        var read = 0L
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
                read += bytesRead
                onProgress(read.toFloat() / total)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

// native_checksum.cpp — C++ JNI layer, wraps the plain-C sha256.c
// implementation and streams a file through it in large chunks. This is the
// actual optimization: hashing a multi-GB GSI image natively is
// significantly faster than the same loop running on the JVM, and avoids
// keeping the whole file in Java heap.

#include <jni.h>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>
#include "sha256.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_vmgsi_app_util_NativeChecksum_sha256OfFile(
    JNIEnv *env, jobject /* this */, jstring filePath) {

    const char *path = env->GetStringUTFChars(filePath, nullptr);

    FILE *f = fopen(path, "rb");
    env->ReleaseStringUTFChars(filePath, path);

    if (!f) {
        return env->NewStringUTF("");
    }

    Sha256Ctx ctx;
    sha256_init(&ctx);

    const size_t bufSize = 1024 * 1024; // 1MB chunks
    std::vector<uint8_t> buffer(bufSize);
    size_t bytesRead;

    while ((bytesRead = fread(buffer.data(), 1, bufSize, f)) > 0) {
        sha256_update(&ctx, buffer.data(), bytesRead);
    }
    fclose(f);

    uint8_t digest[32];
    sha256_final(&ctx, digest);

    char hex[65];
    for (int i = 0; i < 32; i++) {
        snprintf(hex + i * 2, 3, "%02x", digest[i]);
    }
    hex[64] = '\0';

    return env->NewStringUTF(hex);
}

#ifndef VMGSI_SHA256_H
#define VMGSI_SHA256_H

#include <stdint.h>
#include <stddef.h>

// When this header is included from C++ (native_checksum.cpp), the
// declarations below must use C linkage so the symbol names match what
// sha256.c (compiled as plain C) actually exports — without this, the C++
// compiler mangles the names (adds argument-type info) and the linker
// can't find the C implementations, exactly the error that showed up here.
#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    uint32_t state[8];
    uint64_t bitlen;
    uint8_t buffer[64];
    size_t buflen;
} Sha256Ctx;

void sha256_init(Sha256Ctx *ctx);
void sha256_update(Sha256Ctx *ctx, const uint8_t *data, size_t len);
void sha256_final(Sha256Ctx *ctx, uint8_t out[32]);

#ifdef __cplusplus
}
#endif

#endif // VMGSI_SHA256_H

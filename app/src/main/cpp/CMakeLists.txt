cmake_minimum_required(VERSION 3.22.1)
project("vmgsi_native")

# vmgsi_checksum: fast native SHA-256 for verifying downloaded GSI images.
# Kept as its own small, focused library — separate from wherever the QEMU
# native build ends up living — so it can be built/tested independently.
add_library(
    vmgsi_checksum
    SHARED
    native_checksum.cpp
    sha256.c
)

find_library(log-lib log)

target_link_libraries(
    vmgsi_checksum
    ${log-lib}
)

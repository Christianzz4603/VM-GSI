# Prebuilt native libraries

Anything dropped in the matching ABI folder here gets packaged into the APK
as-is by Gradle — no build step, no CMake/cargo involved. This is separate
from `app/src/main/cpp` (our own C/C++, built by CMake) and
`app/src/main/rust` (our own Rust, built by cargo-ndk) — those two compile
from source; this folder is for binaries you already have.

## What goes here

- **QEMU** — once you have a QEMU-for-Android build (see `qemu/QemuLauncher.kt`,
  which expects `libqemu-system-aarch64.so` and `libqemu-system-x86_64.so`),
  the compiled binaries go here. Note the `lib*.so` naming + `.so` extension
  is required even though it's an executable, not a real shared library —
  that's the convention Android uses to let `jniLibs` package and extract
  arbitrary native binaries at install time.
- **magiskboot** — Magisk's official prebuilt `magiskboot` binary (from their
  GitHub releases), same naming convention: `libmagiskboot.so`.

## Layout

```
jniLibs/
├── arm64-v8a/
│   ├── libqemu-system-aarch64.so   (placeholder — not yet added)
│   ├── libqemu-system-x86_64.so    (placeholder — QEMU can emulate x86_64
│   │                                 guests even on an arm64 host, so both
│   │                                 QEMU variants may be needed per host ABI)
│   └── libmagiskboot.so            (placeholder — not yet added)
└── x86_64/
    ├── libqemu-system-aarch64.so
    ├── libqemu-system-x86_64.so
    └── libmagiskboot.so
```

Each ABI folder needs binaries built *for that host ABI* — an arm64 phone
needs the arm64-v8a folder populated, regardless of which guest Android ABI
(arm64 vs x86_64) it's going to boot inside the VM. Don't confuse "host ABI
folder" with "guest ABI the VM boots" — `QemuLauncher.qemuBinaryNameFor()`
picks the QEMU *binary variant* matching the guest, but that binary itself
still needs to be the version compiled for the host's own architecture.

## Executable permission

Android extracts these at install time with executable permission already
set correctly for binaries under `jniLibs` (unlike a raw asset file, which
would need permission fixed up manually at runtime) — this is one of the
main reasons to use this mechanism for QEMU/magiskboot rather than shipping
them as regular assets.

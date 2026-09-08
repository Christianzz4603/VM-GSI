# VM GSI

Android app to download official Google GSI (Generic System Image) builds —
scoped to Android 12 and older — and boot them inside a QEMU-based VM, with
an optional pre-boot Magisk patch so the guest boots pre-rooted.

## Structure

```
app/src/main/java/com/vmgsi/app/
├── data/     GsiRelease, LocalImage, and state models
├── gsi/      GsiCatalog (release list) + GsiDownloader (resumable, checksum-verified)
├── magisk/   MagiskPatcher — pre-boot root patching pipeline
├── qemu/     RootDetector (host KVM check), QemuLauncher, QemuVmService
├── vnc/      (empty — intended to reuse the VNC viewer from the Proot Distro app)
└── ui/       Compose screens: catalog, library, VM/VNC view
```

## What's real vs. stubbed right now

**Scaffolded and functional as written:**
- Gradle project setup (Compose, Room, WorkManager, OkHttp)
- Data models, navigation, screen layouts
- `RootDetector` — actually checks for `su` binaries and `/dev/kvm`
- `GsiDownloader` — real resumable download + SHA-256 verification logic
  (needs the "append on resume" file-open mode wired in for true resume)

**Deliberately stubbed (`TODO()` / placeholders) — needs real work before
this boots anything:**
1. **`GsiCatalog`** — download URLs and SHA-256 hashes are placeholders.
   Needs to be filled from Google's official GSI release page, ideally via
   `fetchLiveManifest()` instead of hardcoding.
2. **`MagiskPatcher`** — the three core steps (extract boot.img, run
   magiskboot, repack) are stubbed. Needs Magisk's official release assets
   bundled and the actual patch sequence implemented against a real boot
   image layout.
3. **QEMU binaries** — this app does not build QEMU. `QemuLauncher` expects
   prebuilt `libqemu-system-{aarch64,x86_64}.so` binaries under
   `app/src/main/jniLibs/<abi>/`. Building QEMU for Android (via NDK,
   targeting ARM64/x86_64 host) is a separate toolchain project.
4. **VNC viewer** — intentionally left as a stub package; should reuse the
   viewer already built for the Proot Distro app instead of a second
   implementation.

## Language split

- **Kotlin** — UI (Compose), navigation, coroutines/Flow-based download logic.
  Default for new Android code.
- **Java** — `util/ByteFormatter.java`. Plain interop, no wrapper needed;
  Kotlin calls it directly. Good spot for any future code ported from an
  existing Java library.
- **C** (`cpp/sha256.c` + `.h`) — SHA-256 implementation, FIPS 180-4, no
  external deps (avoids pulling in OpenSSL for this one algorithm).
- **C++** (`cpp/native_checksum.cpp`) — JNI bridge that streams a file
  through the C SHA-256 code in 1MB chunks. Kotlin calls it via
  `util/NativeChecksum.kt`.
- **Rust** (`rust/src/lib.rs`) — parses the `boot.img` header format to
  locate the ramdisk's byte range, ahead of the Magisk patch step. Chosen
  over C/C++ specifically because this parses a fixed-size binary struct
  out of an **untrusted downloaded file** — exactly the kind of code where
  manual C pointer arithmetic tends to produce buffer-overflow bugs; Rust's
  bounds-checked slices remove that bug class outright. Has real unit tests
  (`cargo test`), run in CI. Bridged to Kotlin via `magisk/NativeBootImage.kt`.
- QEMU and `magiskboot` (once wired in) are also C/C++, same native layer.

No C# — that's the .NET/Xamarin/MAUI ecosystem, a different toolchain from
native Android (JVM + NDK), and mixing it in wouldn't add anything here.

## Optimizations made this pass

- Checksum verification now runs through native C/C++
  (`NativeChecksum.sha256OfFile`) instead of `java.security.MessageDigest`
  — meaningfully faster on multi-GB images, falls back to the Kotlin
  implementation if the native lib fails to load.
- Fixed the download resume path: it now actually opens the output file in
  **append** mode when resuming, instead of silently overwriting from byte 0
  (real bug in the first scaffold pass).
- Larger I/O buffers (256KB) and buffered streams on both the read and write
  side of the download loop.
- Progress emissions are throttled (every ~512KB) instead of firing on every
  chunk, so fast connections don't flood the UI with redundant state updates.

## CI workflow

`.github/workflows/build.yml`, same pattern as your TapMacro repo:
- Every push/PR → unsigned debug APK build, uploaded as an artifact.
- Pushing a tag → signed release APK, using a base64-encoded keystore from
  repo secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) — same
  three secret names as TapMacro, so you can reuse that keystore/secrets
  setup if you want one signing identity across your apps, or generate a
  fresh keystore for this repo instead.

**Before this workflow will actually run:** the real `gradlew` script and
`gradle-wrapper.jar` binary aren't included (can't be generated without
network access here) — only `gradle/wrapper/gradle-wrapper.properties`.
Once this is pushed to a repo, run `gradle wrapper` once (e.g. from Termux,
same as your other projects) to generate those two files, commit them, and
the workflow will build. You'll also need `cargo-ndk` and the Rust Android
targets locally if building outside CI:
```
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk
```

## Native library packaging

Three source folders feed into the final `.so` files packaged in the APK:

| Folder | Contains | Built by |
|---|---|---|
| `cpp/` | Our C/C++ source (checksum) | CMake, at Gradle build time |
| `rust/` | Our Rust source (boot image parser) | cargo-ndk, at Gradle build time |
| `jniLibs/<abi>/` | Prebuilt binaries we didn't compile (QEMU, magiskboot) | Nothing — packaged as-is |

All three end up merged into the same per-ABI native library set in the
final APK — Gradle doesn't care which folder a `.so` came from. See
`jniLibs/README.md` for the prebuilt-binary naming convention
(`lib<name>.so`, required even for executables like QEMU).

## Icons / visual assets

All vector drawables (Android's native scalable-vector format — same idea
as SVG, XML `<path>` data instead of bitmaps, so no per-density PNG exports
needed):

- `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` — adaptive
  launcher icon (background color + `ic_launcher_foreground.xml`, a simple
  chip/VM mark). Safe to rely on adaptive-icon-only since minSdk is 26,
  the same API level adaptive icons were introduced in — no legacy PNG
  fallback needed.
- `drawable/ic_download.xml` — used on the catalog screen's Download button
- `drawable/ic_root_shield.xml` — used on "Download rooted"
- `drawable/ic_vm_boot.xml` — used on the VM/boot screen
- `drawable/ic_verified.xml` — not wired in yet; intended for the
  checksum-verified state once `LibraryScreen` shows real download progress

Swap `ic_launcher_foreground.xml` for a real designed mark whenever you
have one — it's a placeholder shape, not a final logo.

## Next steps (suggested order)

1. Fill in real `GsiCatalog` data (or build `fetchLiveManifest()`) so
   downloads actually point somewhere real.
2. Wire the VNC viewer in from Proot Distro.
3. Get a QEMU Android build producing the two `.so` binaries — this can be
   tested completely independently of the rest of the app.
4. Only then tackle `MagiskPatcher`, since it's the most fiddly piece and
   easiest to validate once you can already boot an *unpatched* image
   end-to-end.

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.vmgsi.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vmgsi.app"
        // Host device minimum — separate from which GSI version the app can boot.
        // Android 8.0 (API 26) is Project Treble's baseline, so it's a natural floor
        // since GSI itself only exists on Treble devices.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-O2"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "26.3.11579264"

    sourceSets {
        getByName("main") {
            // Rust builds its .so output here; cargoNdkBuild (below) populates
            // this directory before the native libs get merged into the APK.
            jniLibs.srcDir("build/rustJniLibs")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        // QEMU native binaries are large; keep them uncompressed so they can be
        // executed directly from the APK/AAB without extraction overhead.
        jniLibs.useLegacyPackaging = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Background downloads for large GSI images (multi-GB, resumable)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Networking for fetching GSI manifests / images from Google's servers
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Local index of downloaded/patched images
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
}

// Builds the Rust boot-image-parser crate (app/src/main/rust) for both
// target ABIs via cargo-ndk, dropping the resulting .so files where the
// jniLibs.srcDir above expects them. Requires `cargo install cargo-ndk` and
// the Rust Android targets (`rustup target add aarch64-linux-android
// x86_64-linux-android`) on the machine/CI runner building this.
tasks.register<Exec>("cargoNdkBuild") {
    workingDir = file("src/main/rust")
    commandLine(
        "cargo", "ndk",
        "-t", "arm64-v8a",
        "-t", "x86_64",
        "-o", "${layout.buildDirectory.get()}/rustJniLibs",
        "build", "--release",
    )
}

tasks.named("preBuild") {
    dependsOn("cargoNdkBuild")
}

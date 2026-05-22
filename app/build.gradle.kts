import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Move build outputs outside OneDrive — OneDrive sync corrupts/locks files
// (e.g. KSP generated dirs and packaged manifests) mid-build. Only applies on
// the local Windows dev box; CI (Linux) uses the default `app/build` directory.
run {
    val override = System.getenv("ANDROID_BUILD_DIR")
        ?: if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
            && file("C:/CCBuild").exists()
        ) "C:/CCBuild/app" else null
    if (override != null) layout.buildDirectory.set(file(override))
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(name: String, default: String = "") =
    (localProps.getProperty(name) ?: System.getenv(name) ?: default)
        .trim()
        .trim('"', '\'')

android {
    namespace = "com.app.stash.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.stash.android"
        minSdk = 26
        targetSdk = 36
        // versionCode/versionName can be overridden by CI via env vars
        // (set from the git tag in release.yml). Falls back to 1 / "1.0" locally.
        versionCode = (System.getenv("RELEASE_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("RELEASE_VERSION_NAME") ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            "String",
            "FOURSQUARE_API_KEY",
            "\"${localProp("FOURSQUARE_API_KEY")}\""
        )
    }

    signingConfigs {
        // Stable signing key for consistent APK signatures across releases.
        // Credentials are read from local.properties (not committed) or
        // environment variables so they never appear in version control.
        //
        // Dependabot PRs and fork PRs don't have access to repo secrets, so
        // SIGNING_STORE_PASSWORD will be empty — in that case we skip
        // creating the config and let AGP fall back to the auto-generated
        // debug keystore. Release builds in release.yml always have the
        // secrets and so always use the upgrade key.
        val storePwd = localProp("SIGNING_STORE_PASSWORD")
        if (storePwd.isNotEmpty()) {
            create("upgrade") {
                storeFile = file(localProp("SIGNING_STORE_FILE", "upgrade.keystore"))
                storePassword = storePwd
                keyAlias = localProp("SIGNING_KEY_ALIAS", "upgrade")
                keyPassword = localProp("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Keep minify off for now — enabling without tested ProGuard rules
            // tends to strip Hilt/Room/Retrofit/SQLCipher reflection targets.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfigs.findByName("upgrade")?.let { signingConfig = it }
        }
        debug {
            isMinifyEnabled = false
            // Sign debug with the same upgrade key (when available) so
            // debug ↔ release APKs are install-compatible (no data wipe
            // when switching). Without secrets, AGP's default debug
            // keystore is used.
            signingConfigs.findByName("upgrade")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.navigation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.sqlcipher)
    ksp(libs.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.work.runtime.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.play.services.location)

    implementation(libs.plaid.link)
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.biometric)
    implementation(libs.coil.compose)
    implementation(libs.osmdroid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "fr.avantdevoter.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "fr.avantdevoter.app"
        minSdk = 29 // Android 10
        targetSdk = 36
        // Numéro de version : fourni par GitHub Actions lors de la fabrication de l'APK.
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
        // Adresse des fichiers publiés chaque matin par le robot (GitHub Pages).
        val dataUrl = (project.findProperty("dataUrl") as String?) ?: "https://spawn2929.github.io/avant-de-voter/"
        buildConfigField("String", "DATA_URL", "\"$dataUrl\"")
        buildConfigField("String", "GITHUB_REPO", "\"Spawn2929/avant-de-voter\"")
    }

    // Clé de signature de l'APK définitif : variables d'environnement sur GitHub Actions,
    // sinon secrets/keystore.properties sur le PC. Jamais dans le dépôt.
    val keystoreProps = rootProject.file("../secrets/keystore.properties")
    val signing = when {
        System.getenv("SIGNING_STORE_FILE") != null -> mapOf(
            "storeFile" to file(System.getenv("SIGNING_STORE_FILE")),
            "storePassword" to System.getenv("SIGNING_STORE_PASSWORD"),
            "keyAlias" to System.getenv("SIGNING_KEY_ALIAS"),
            "keyPassword" to System.getenv("SIGNING_KEY_PASSWORD"),
        )
        keystoreProps.exists() -> Properties().apply { keystoreProps.inputStream().use(::load) }.let { p ->
            mapOf(
                "storeFile" to keystoreProps.parentFile.resolve(p.getProperty("storeFile")),
                "storePassword" to p.getProperty("storePassword"),
                "keyAlias" to p.getProperty("keyAlias"),
                "keyPassword" to p.getProperty("keyPassword"),
            )
        }
        else -> null
    }

    signingConfigs {
        if (signing != null) {
            create("release") {
                storeFile = signing["storeFile"] as File
                storePassword = signing["storePassword"] as String
                keyAlias = signing["keyAlias"] as String
                keyPassword = signing["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Retire le code et les icônes inutilisés : APK bien plus léger.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.11.0")

    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")

    testImplementation("junit:junit:4.13.2")
}

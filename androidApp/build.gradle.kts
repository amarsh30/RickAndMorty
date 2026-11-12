import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.koin.android)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.amr.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.amr.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Optional upload signing from the environment (CI secret or local shell); never stored in
    // the repo. Without all four values the release stays unsigned: no debug-key fallback.
    val uploadStoreFile = System.getenv("RM_UPLOAD_STORE_FILE")
    val uploadStorePassword = System.getenv("RM_UPLOAD_STORE_PASSWORD")
    val uploadKeyAlias = System.getenv("RM_UPLOAD_KEY_ALIAS")
    val uploadKeyPassword = System.getenv("RM_UPLOAD_KEY_PASSWORD")
    val hasUploadSigning = !uploadStoreFile.isNullOrBlank() && !uploadStorePassword.isNullOrBlank() &&
        !uploadKeyAlias.isNullOrBlank() && !uploadKeyPassword.isNullOrBlank()

    signingConfigs {
        if (hasUploadSigning) {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasUploadSigning) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}
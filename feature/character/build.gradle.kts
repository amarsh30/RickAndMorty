plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("amr.kmp.test")
}

kotlin {
    android {
        namespace = "com.amr.feature.character"

        compileSdk {
            version = release(37)
        }

        minSdk = 24
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.androidx.lifecycle.viewmodel)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            // Domain models dan UseCases
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
        }
    }
}

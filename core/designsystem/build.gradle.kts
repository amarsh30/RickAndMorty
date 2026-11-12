plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Host/device/UI tests for the theme and shared components.
    id("amr.kmp.test")
}

kotlin {
    android {
        namespace = "com.amr.core.designsystem"

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

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            // ImageVector icons and graphics (already in the catalog).
            implementation(libs.compose.ui)
        }
    }
}
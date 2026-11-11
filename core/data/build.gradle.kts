plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("amr.kmp.test")
}

kotlin {
    android {
        namespace = "com.amr.core.data"

        compileSdk {
            version = release(37)
        }

        minSdk = 24
    }

    //iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)

            implementation(project(":core:domain"))
            implementation(project(":core:network"))
            implementation(project(":core:database"))

            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
        }
    }
}

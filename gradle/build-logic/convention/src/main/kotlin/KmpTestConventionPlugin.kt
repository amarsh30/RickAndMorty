import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin untuk automation testing di modul KMP library.
 *
 * Cukup tambahkan di build.gradle.kts modul (setelah plugin KMP & Android):
 * ```
 * plugins {
 *     alias(libs.plugins.kotlinMultiplatform)
 *     alias(libs.plugins.androidMultiplatformLibrary)
 *     id("amr.kmp.test")
 * }
 * ```
 *
 * Yang otomatis disiapkan:
 * - Host test (JVM) dan device test (emulator) untuk commonTest.
 * - Dependency: kotlin-test, kotlinx-coroutines-test, AndroidJUnitRunner.
 * - Jika modul memakai Compose: dependency UI test + androidResources.
 * - Task per modul: unitTest, deviceTest, allTest (dan uiTest untuk modul Compose).
 *   Menjalankan `./gradlew unitTest` dari root akan mengeksekusi task tersebut
 *   di SEMUA modul yang memakai plugin ini.
 */
class KmpTestConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
            val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
            val android = (kotlin as ExtensionAware).extensions
                .getByType<KotlinMultiplatformAndroidLibraryExtension>()

            configureTestTargets(android)
            configureTestDependencies(kotlin, libs)
            registerTestTasks()

            pluginManager.withPlugin("org.jetbrains.compose") {
                configureComposeUiTest(android, kotlin, libs)
            }
        }
    }

    private fun configureTestTargets(android: KotlinMultiplatformAndroidLibraryExtension) {
        // Unit test di JVM lokal → task testAndroidHostTest
        android.withHostTest {
            isIncludeAndroidResources = true
        }

        // commonTest juga dijalankan di emulator → task connectedAndroidDeviceTest
        android.withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    private fun configureTestDependencies(
        kotlin: KotlinMultiplatformExtension,
        libs: VersionCatalog
    ) {
        kotlin.sourceSets.getByName("commonTest").dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
            implementation(libs.findLibrary("kotlinx-coroutines-test").get())
        }
        kotlin.sourceSets.getByName("androidDeviceTest").dependencies {
            implementation(libs.findLibrary("androidx-testRunner").get())
        }
    }

    private fun Project.configureComposeUiTest(
        android: KotlinMultiplatformAndroidLibraryExtension,
        kotlin: KotlinMultiplatformExtension,
        libs: VersionCatalog
    ) {
        // Dibutuhkan plugin Compose Resources agar task device test punya folder assets
        android.androidResources.enable = true

        kotlin.sourceSets.getByName("androidDeviceTest").dependencies {
            implementation(libs.findLibrary("compose-ui-test").get())
            // Menyediakan ComponentActivity kosong sebagai "wadah" layar saat test
            implementation(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            // Espresso lama (bawaan ui-test) crash di Android 17: InputManager.getInstance sudah dihapus
            implementation(libs.findLibrary("androidx-espresso-core").get())
        }

        tasks.register("uiTest") {
            group = "verification"
            description = "Menjalankan UI test Compose (dan test lain di modul ini) di emulator/HP."
            dependsOn("connectedAndroidDeviceTest")
        }
    }

    private fun Project.registerTestTasks() {
        tasks.register("unitTest") {
            group = "verification"
            description = "Menjalankan unit test di JVM (tanpa emulator)."
            dependsOn("testAndroidHostTest")
        }
        tasks.register("deviceTest") {
            group = "verification"
            description = "Menjalankan semua test di emulator/HP yang terhubung."
            dependsOn("connectedAndroidDeviceTest")
        }
        tasks.register("allTest") {
            group = "verification"
            description = "Menjalankan unitTest lalu deviceTest."
            dependsOn("unitTest", "deviceTest")
        }
    }
}

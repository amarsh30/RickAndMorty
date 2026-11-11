import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin untuk modul feature (UI Compose + ViewModel) di atas "amr.kmp.library".
 *
 * ```
 * plugins {
 *     id("amr.kmp.feature")
 * }
 * ```
 *
 * Yang disiapkan, karena dipakai setiap feature:
 * - Compose Multiplatform + compiler (dan UI test dari "amr.kmp.test").
 * - Compose runtime/foundation/material3, lifecycle ViewModel, Koin, coroutines.
 * - Modul :core:domain dan :core:designsystem.
 *
 * Dependency yang hanya dipakai sebagian feature (mis. Coil) tetap ditulis
 * eksplisit di build.gradle.kts modulnya.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.apply("amr.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        kotlin.sourceSets.getByName("commonMain").dependencies {
            listOf(
                "kotlinx-coroutines-core",
                "compose-runtime",
                "compose-foundation",
                "compose-material3",
                "compose-ui",
                "androidx-lifecycle-viewmodel",
                "androidx-lifecycle-runtimeCompose",
                "koin-core",
                "koin-compose-viewmodel"
            ).forEach { alias -> implementation(libs.findLibrary(alias).get()) }

            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
        }
    }
}

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin untuk modul library KMP (Android + iOS).
 *
 * ```
 * plugins {
 *     id("amr.kmp.library")
 * }
 *
 * kotlin {
 *     android { namespace = "com.amr.xxx" }   // namespace tetap per modul
 * }
 * ```
 *
 * Yang disiapkan:
 * - Plugin Kotlin Multiplatform + Android KMP library.
 * - Target Android dengan compileSdk/minSdk dari version catalog.
 * - Target iosArm64 dan iosSimulatorArm64.
 * - Setup test dari "amr.kmp.test".
 */
class KmpLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("amr.kmp.test")

        val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
        (kotlin as ExtensionAware).extensions
            .getByType<KotlinMultiplatformAndroidLibraryExtension>()
            .apply {
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
                minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
            }

        kotlin.iosArm64()
        kotlin.iosSimulatorArm64()

        kotlin.sourceSets.getByName("commonMain").dependencies {
            implementation(libs.findLibrary("kotlin-stdlib").get())
        }
    }
}

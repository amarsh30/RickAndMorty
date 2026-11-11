plugins {
    `kotlin-dsl`
}

dependencies {
    // compileOnly: versi plugin tetap diatur oleh build utama (root build.gradle.kts)
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpTest") {
            id = "amr.kmp.test"
            implementationClass = "KmpTestConventionPlugin"
        }
        register("kmpLibrary") {
            id = "amr.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpFeature") {
            id = "amr.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
    }
}

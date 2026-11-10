plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidLint) apply false
}

// Task automation testing (unitTest, uiTest, deviceTest, allTest) didaftarkan per modul
// oleh convention plugin "amr.kmp.test" (gradle/build-logic). Menjalankan
// `./gradlew unitTest` dari root otomatis mengeksekusinya di semua modul. Lihat TESTING.md.

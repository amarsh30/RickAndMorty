// Included build berisi convention plugin (konfigurasi Gradle yang dipakai bersama antar modul).
// Didaftarkan di settings.gradle.kts root lewat includeBuild("gradle/build-logic").

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
}

// Nama dibedakan dari modul template :build-logic di build utama agar tidak bentrok
rootProject.name = "convention-plugins"
include(":convention")

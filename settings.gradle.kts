rootProject.name = "RickAndMorty"

pluginManagement {
    // Convention plugin lokal (mis. id("amr.kmp.test")), lihat gradle/build-logic
    // Nama dibedakan dari modul template :build-logic agar tidak bentrok
    includeBuild("gradle/build-logic") {
        name = "convention-plugins"
    }

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":shared")
include(":core")
include(":feature")
include(":build-logic")

include(":core:domain")
include(":core:network")
include(":core:data")
include(":core:database")
include(":core:designsystem")

include(":feature:character")

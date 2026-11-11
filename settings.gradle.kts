rootProject.name = "RickAndMorty"

pluginManagement {
    // Convention plugin lokal (mis. id("amr.kmp.test")), lihat gradle/build-logic
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

include(":core:domain")
include(":core:network")
include(":core:data")
include(":core:database")
include(":core:designsystem")

include(":feature:character")
include(":feature:location")
include(":feature:episode")

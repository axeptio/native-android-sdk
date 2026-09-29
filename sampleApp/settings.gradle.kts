pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The Axeptio SDK, served from the static Maven repo committed alongside
        // this example in the distribution repo. If you copy this project out of
        // the repository, use the remote URL your own app would use instead:
        // maven { url = uri("https://raw.githubusercontent.com/axeptio/native-android-sdk/master/maven") }
        maven {
            url = uri("../maven")
            content { includeGroup("io.axeptio") }
        }
    }
}

rootProject.name = "axeptio-sdk-example"
include(":app")

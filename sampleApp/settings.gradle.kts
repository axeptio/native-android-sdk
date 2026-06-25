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
        // this example in the distribution repo. When the example is run from
        // inside the SDK sources, `../maven` is produced by `./gradlew publish`.
        maven { url = uri("../maven") }
    }
}

rootProject.name = "axeptio-sdk-example"
include(":app")

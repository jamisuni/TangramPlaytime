// TangramPlaytime — one Gradle module per code home (architecture.md §3, ADR-002; build-map.md).
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TangramPlaytime"

include(":contracts")
include(":kernel")
include(":content")
include(":store")
include(":play")
include(":app")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "FortunePocket"

include(":app")
include(":core:core-model")
include(":core:core-data")
include(":core:core-ui")
include(":core:core-content")
include(":feature:feature-home")
include(":feature:feature-tarot")
include(":feature:feature-astrology")
include(":feature:feature-bazi")
include(":feature:feature-history")
include(":feature:feature-settings")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.android.application") version "8.3.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ARWallCanvas"
include(":app")
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ARWallCanvas"
include(":app")

rootProject.name = "ARWallCanvas"
include(":app")

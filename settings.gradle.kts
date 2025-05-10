pluginManagement {
    // Quarkus plugin (you already had this)
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings

    repositories {
        // Gradle Plugin Portal first for community plugins
        gradlePluginPortal()
        // Google’s Maven for Android Gradle Plugin
        google()
        // Central repositories for other plugins
        mavenCentral()
        mavenLocal()
    }

    plugins {
        // Quarkus plugin
        id(quarkusPluginId) version quarkusPluginVersion

        // Pin Android and Kotlin plugin versions so your aliases resolve correctly
        id("com.android.application") version "8.9.1"
        id("org.jetbrains.kotlin.android") version "1.8.21"
        id("org.jetbrains.kotlin.plugin.compose") version "1.8.21"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "catania-united"
//include(":app")
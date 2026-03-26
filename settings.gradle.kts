pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.11"
}

stonecutter {
    create(rootProject) {
        // Support 1.21.9-26.1
        versions("1.21.9", "1.21.10", "1.21.11", "26.1")
        vcsVersion = "26.1"
    }
}

rootProject.name = "SimpleDeathBans"

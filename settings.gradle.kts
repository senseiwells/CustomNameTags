rootProject.name = "CustomNameTags"
pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }

    val loomVersion: String by settings
    val fabricKotlinVersion: String by settings
    plugins {
        id("io.github.juuxel.loom-quiltflower") version "1.7.3"
        id("fabric-loom") version loomVersion
        id("org.jetbrains.kotlin.jvm") version
                fabricKotlinVersion
                    .split("+kotlin.")[1] // Grabs the sentence after `+kotlin.`
                    .split("+")[0] // Ensures sentences like `+build.1` are ignored
    }
}
plugins {
    val jvmVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]

    kotlin("jvm").version(jvmVersion)
    kotlin("plugin.serialization").version(jvmVersion)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.mod.publish)
    `maven-publish`
    java
}

val modVersion = "1.5.2"
val releaseVersion = "${modVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

repositories {
    mavenCentral()
    maven("https://maven.supersanta.me/snapshots")
    maven("https://maven.parchmentmc.org/")
    maven("https://jitpack.io")
    maven("https://maven.nucleoid.xyz")
    mavenLocal()
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)

    implementation(libs.placeholder)

    include(libs.bundles.arcade)
    implementation(libs.bundles.arcade)

    include(implementation(libs.predicate.get())!!)

//    localRuntime(libs.puppets)
}

loom {
    runs {
        getByName("server") {
            runDirectory.set(file("run/${libs.versions.minecraft.get()}"))
        }
    }
}

java {
    withSourcesJar()
}

tasks {
    processResources {
        inputs.property("version", modVersion)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf(
                "version" to modVersion,
                "fabric_loader_dependency" to libs.versions.fabric.loader.get(),
                "fabric_kotlin_dependency" to libs.versions.fabric.kotlin.get(),
                "minecraft_dependency" to "~${libs.versions.minecraft.get()}",
                "placeholder_dependency" to libs.versions.placeholder.get(),
            ))
        }
    }

    jar {
        from("LICENSE")
    }

    publishMods {
        file = jar.get().archiveFile
        changelog.set(
            """
            - Fix crashing at startup
            """.trimIndent()
        )
        type = STABLE
        modLoaders.add("fabric")

        displayName = "CustomNameTags $modVersion for ${libs.versions.minecraft.get()}"
        version = releaseVersion

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_API_KEY")
            projectId = "TizFPouK"
            minecraftVersions.add(libs.versions.minecraft)

            projectDescription.set(file("README.md").readText())

            requires {
                slug = "fabric-api"
            }
            requires {
                slug = "fabric-language-kotlin"
            }
            requires {
                slug = "placeholder-api"
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("nametags") {
            artifactId = "custom-nametags"
            from(components["java"])
        }
    }

    repositories {
        val mavenUrl = System.getenv("MAVEN_URL")
        if (mavenUrl != null) {
            maven {
                url = uri(mavenUrl)
                val mavenUsername = System.getenv("MAVEN_USERNAME")
                val mavenPassword = System.getenv("MAVEN_PASSWORD")
                if (mavenUsername != null && mavenPassword != null) {
                    credentials {
                        username = mavenUsername
                        password = mavenPassword
                    }
                }
            }
        }
    }
}
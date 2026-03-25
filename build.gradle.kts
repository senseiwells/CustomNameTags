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

val modVersion = "1.3.1"
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

    include(implementation(libs.arcade.nametags.get())!!)
    include(implementation(libs.arcade.commands.get())!!)
    include(implementation(libs.arcade.extensions.get())!!)
    include(implementation(libs.arcade.event.registry.get())!!)
    include(implementation(libs.arcade.events.server.get())!!)
    include(implementation(libs.arcade.utils.get())!!)
    include(implementation(libs.arcade.virtual.entities.get())!!)

    include(implementation(libs.predicate.get())!!)

    include(implementation(libs.permissions.get())!!)

    localRuntime(libs.puppets)
}

loom {
    runs {
        getByName("server") {
            runDir = "run/${libs.versions.minecraft.get()}"
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
                "minecraft_dependency" to libs.versions.minecraft.get(),
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
            - Fix some datafixer issues
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

            requires {
                id = "Ha28R6CL"
            }
            requires {
                id = "P7dR8mSH"
            }
            requires {
                id = "eXts2L7r"
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
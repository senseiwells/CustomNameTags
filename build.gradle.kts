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

val modVersion = "2.0.0-beta.1"
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
    include(implementation(libs.simple.config.get())!!)

    localRuntime(libs.puppets)
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
                "minecraft_dependency" to replaceVersion(libs.versions.minecraft.get(), "x"),
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
            This version contains a redesign of the nametag config system
            
            - Added per-player nametag overrides. Now a defined nametag can be completely
              overridden on a per-player basis. Most usefully, you can override what
              text displays for specific players, as well as whether or not nametags
              are attached to specific players. See the updated mod page for more information. 
            - Nametags now have priority, nametags will still render in the order they are
              defined in the config, but this can be further controlled via a specific priority value
            - Expand the `/nametag` command to allow for more in-game configuration
              - Added the `/nametag edit` command to edit a nametags text, who it attaches to
                and its priority
              - Added the `/nametag player` command to edit per-player overrides
            - Migrated to version 2 of the config
              - The format of the config has changed, see the mod page for specifics
              - All old config files will be automatically updated (and backed up just in case)
              - Invalid configs are now backed up instead of just being overwritten
            """.trimIndent()
        )
        type = BETA
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

private val minecraftVersionRegex = Regex("""^(\d+\.\d+)(\.\d+)?(?:-(pre|rc)-?(\d+))?$""")

fun replaceVersion(version: String, patch: String): String {
    val match = minecraftVersionRegex.matchEntire(version)
        ?: throw IllegalArgumentException("Unrecognised Minecraft version: $version")
    val (minor, patchVersion, type, number) = match.destructured
    if (type.isEmpty()) {
        return "$minor.$patch"
    }
    return "$minor$patchVersion-$type.$number"
}
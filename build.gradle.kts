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

val modVersion = "0.4.5"
val releaseVersion = "${modVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.supersanta.me/snapshots")
    maven("https://maven.parchmentmc.org/")
    maven("https://jitpack.io")
    maven("https://maven.nucleoid.xyz")
    maven("https://maven.andante.dev/releases/")
}

@Suppress("UnstableApiUsage")
dependencies {
    minecraft(libs.minecraft)
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${libs.versions.parchment.get()}@zip")
    })

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.kotlin)

    modCompileOnly(libs.server.replay)

    modApi(libs.polymer.core)
    modApi(libs.polymer.virtual.entity)
    modImplementation(libs.placeholder)
    includeModImplementation(libs.predicate) {}

    includeModImplementation(libs.permissions) {
        exclude(libs.fabric.api.get().group)
    }
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
                "minecraft_dependency" to libs.versions.minecraft.get().replaceAfterLast('.', "x"),
                "polymer_dependency" to libs.versions.polymer.get(),
                "placeholder_dependency" to libs.versions.placeholder.get()
            ))
        }
    }

    jar {
        from("LICENSE")
    }

    publishMods {
        file = remapJar.get().archiveFile
        changelog.set(
            """
            Updated to 1.21.5
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
                id = "xGdtZczs"
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

private fun DependencyHandler.includeModImplementation(provider: Provider<*>, action: Action<ExternalModuleDependency>) {
    include(provider, action)
    modImplementation(provider, action)
}
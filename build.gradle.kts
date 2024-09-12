plugins {
    val jvmVersion = libs.versions.fabric.kotlin.get()
        .split("+kotlin.")[1]
        .split("+")[0]

    kotlin("jvm").version(jvmVersion)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.mod.publish)
    `maven-publish`
    java
}

val modVersion = "0.2.3"
val releaseVersion = "${modVersion}+mc${libs.versions.minecraft.get()}"
val mavenVersion = "${modVersion}+${libs.versions.minecraft.get()}"
version = releaseVersion
group = "me.senseiwells"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.supersanta.me/snapshots")
    maven("https://maven.parchmentmc.org/")
    maven("https://jitpack.io")
    maven("https://ueaj.dev/maven")
    maven("https://maven.nucleoid.xyz")
    maven("https://maven.maxhenkel.de/repository/public")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
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

    modImplementation(libs.server.replay)

    includeModImplementation(libs.polymer.core)
    includeModImplementation(libs.polymer.virtual.entity)
    includeModImplementation(libs.placeholder)
    includeModImplementation(libs.predicate)

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
            expand(mutableMapOf("version" to modVersion))
        }
    }

    jar {
        from("LICENSE")
    }

    publishMods {
        file = remapJar.get().archiveFile
        changelog.set(
            """
            - Update to 1.21
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
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("nametags") {
            groupId = "me.senseiwells"
            artifactId = "custom-nametags"
            version = mavenVersion
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

private fun DependencyHandler.includeModImplementation(dependencyNotation: Any) {
    include(dependencyNotation)
    modImplementation(dependencyNotation)
}

private fun DependencyHandler.includeModImplementation(provider: Provider<*>, action: Action<ExternalModuleDependency>) {
    include(provider, action)
    modImplementation(provider, action)
}
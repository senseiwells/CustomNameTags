import org.apache.commons.io.output.ByteArrayOutputStream
import java.nio.charset.Charset

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
    id("fabric-loom")
    `maven-publish`
    java
    id("me.modmuss50.mod-publish-plugin").version("0.3.4")
}

group = property("maven_group")!!
version = property("mod_version")!!

val releaseVersion = "${project.version}+mc${project.property("minecraft_version")}"

repositories {
    maven("https://maven.parchmentmc.org/")
    maven("https://jitpack.io")
    maven("https://ueaj.dev/maven")
    maven("https://maven.nucleoid.xyz")
    maven("https://maven.maxhenkel.de/repository/public")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
}

@Suppress("UnstableApiUsage")
dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${property("parchment_version")}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    include(modImplementation("eu.pb4:polymer-core:${property("polymer_version")}")!!)
    include(modImplementation("eu.pb4:polymer-virtual-entity:${property("polymer_version")}")!!)
    include(modImplementation("eu.pb4:placeholder-api:${property("placeholder_version")}")!!)
    include(modImplementation("eu.pb4:predicate-api:${property("predicate_api_version")}")!!)

    include(modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")!!)
    // include(implementation(annotationProcessor("com.github.llamalad7.mixinextras:mixinextras-fabric:${property("mixin_extras_version")}")!!)!!)

    modImplementation("com.github.senseiwells:ServerReplay:${property("server_replay_version")}")
}

loom {
    runs {
        getByName("server") {
            runDir = "run/${project.property("minecraft_version")}"
        }
    }
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version))
        }
    }

    remapJar {
        archiveVersion.set(releaseVersion)
    }

    remapSourcesJar {
        archiveVersion.set(releaseVersion)
    }

    jar {
        from("LICENSE")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                groupId = "com.github.senseiwells"
                artifactId = "CustomNameTags"
                version = getGitHash()
                from(project.components.getByName("java"))
            }
        }
    }
}

java {
    withSourcesJar()
}

publishMods {
    file.set(tasks.remapJar.get().archiveFile)
    changelog.set("""
    - Reworked the entire nametag system
        - This fixes a visual bug when viewing nametags from above.
    - Added `"view_radius"` which lets you specify how far players can view a nametag before it disappearing
	""".trimIndent())
    type.set(STABLE)
    modLoaders.add("fabric")

    displayName.set("CustomNameTags ${property("minecraft_version")} v${project.version}")

    modrinth {
        accessToken.set(providers.environmentVariable("MODRINTH_API_KEY"))
        projectId.set("TizFPouK")
        minecraftVersions.add(property("minecraft_version").toString())

        requires {
            id.set("Ha28R6CL")
        }
        requires {
            id.set("P7dR8mSH")
        }
    }
}

fun getGitHash(): String {
    val out = ByteArrayOutputStream()
    exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = out
    }
    return out.toString(Charset.defaultCharset()).trim()
}
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

val modVersion: String by project

val mcVersion: String by project
val parchmentVersion: String by project
val loaderVersion: String by project
val fabricVersion: String by project
val fabricKotlinVersion: String by project

val polymerVersion: String by project
val placeholderVersion: String by project
val predicateApiVersion: String by project
// val serverReplayVersion: String by project

version = "${modVersion}+mc${mcVersion}"
group = "me.senseiwells"

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
    minecraft("com.mojang:minecraft:${mcVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentVersion}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${fabricKotlinVersion}")

    include(modImplementation("eu.pb4:polymer-core:${polymerVersion}")!!)
    include(modImplementation("eu.pb4:polymer-virtual-entity:${polymerVersion}")!!)
    include(modImplementation("eu.pb4:placeholder-api:${placeholderVersion}")!!)
    include(modImplementation("eu.pb4:predicate-api:${predicateApiVersion}")!!)

    include(modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")!!)

    // modImplementation("com.github.senseiwells:ServerReplay:${serverReplayVersion}")
}

loom {
    runs {
        getByName("server") {
            runDir = "run/${mcVersion}"
        }
    }
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
    - Update to 1.20.5
	""".trimIndent())
    type.set(STABLE)
    modLoaders.add("fabric")

    displayName.set("CustomNameTags $mcVersion v$modVersion")

    modrinth {
        accessToken.set(providers.environmentVariable("MODRINTH_API_KEY"))
        projectId.set("TizFPouK")
        minecraftVersions.add(mcVersion)

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
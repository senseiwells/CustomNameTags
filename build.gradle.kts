plugins {
    kotlin("jvm")
    id("fabric-loom")
    `maven-publish`
    java
    id("me.modmuss50.mod-publish-plugin").version("0.3.4")
}

group = property("maven_group")!!
version = property("mod_version")!!

repositories {
    maven {
        url = uri("https://maven.parchmentmc.org/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://ueaj.dev/maven")
    }
    maven {
        url = uri("https://maven.nucleoid.xyz")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
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
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version))
        }
    }

    jar {
        from("LICENSE")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}

java {
    withSourcesJar()
}

publishMods {
    file.set(tasks.remapJar.get().archiveFile)
    changelog.set("""
    - Updated to support 1.20.4
	""")
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
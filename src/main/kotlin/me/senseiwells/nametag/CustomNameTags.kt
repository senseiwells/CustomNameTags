package me.senseiwells.nametag

import kotlinx.io.IOException
import me.senseiwells.nametag.impl.NametagCommand
import me.senseiwells.nametag.impl.PlaceholderNametag
import me.senseiwells.nametag.impl.config.NametagConfig
import me.senseiwells.nametag.impl.placeholder.ExtraPlayerPlaceholders
import me.senseiwells.nametag.impl.predicate.ExtraPredicates
import net.casual.arcade.commands.register
import net.casual.arcade.events.GlobalEventHandler
import net.casual.arcade.events.server.ServerRegisterCommandEvent
import net.casual.arcade.events.server.ServerStartEvent
import net.casual.arcade.events.server.player.PlayerJoinEvent
import net.casual.arcade.events.utils.register
import net.casual.arcade.nametags.extensions.EntityNametagExtension.Companion.nametagExtension
import net.casual.arcade.utils.Identifier
import net.casual.arcade.utils.JsonUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.*

object CustomNameTags: ModInitializer {
    const val MOD_ID = "custom-nametags"

    private val configPath = FabricLoader.getInstance().configDir.resolve(MOD_ID)
    private val logger: Logger = LoggerFactory.getLogger(MOD_ID)

    private lateinit var config: NametagConfig

    fun id(path: String): Identifier {
        return Identifier(MOD_ID, path)
    }

    override fun onInitialize() {
        ExtraPlayerPlaceholders.register()
        ExtraPredicates.register()

        this.migrateOldConfigs()

        GlobalEventHandler.Server.register<ServerStartEvent> { (server) ->
            this.readConfig(server)
        }
        GlobalEventHandler.Server.register<ServerRegisterCommandEvent> {
            it.register(NametagCommand)
        }
        GlobalEventHandler.Server.register<PlayerJoinEvent> { (player) ->
            for (nametag in this.getNametags()) {
                player.nametagExtension.add(nametag)
            }
        }
    }

    fun addNametag(id: Identifier, nametag: PlaceholderNametag) {
        this.config.nametags[id] = nametag
    }

    fun removeNametag(id: Identifier): PlaceholderNametag? {
        return this.config.nametags.remove(id)
    }

    fun getNametagIds(): Set<Identifier> {
        return this.config.nametags.keys
    }

    fun getNametags(): Collection<PlaceholderNametag> {
        return this.config.nametags.values
    }

    fun readConfig(server: MinecraftServer) {
        val config = this.configPath.resolve("config.json")
        if (!config.isRegularFile()) {
            this.logger.info("Generating default config")
            this.config = NametagConfig()
            this.writeConfig(server)
            return
        }

        try {
            this.config = JsonUtils.decodeWith(NametagConfig.CODEC, config, server.registryAccess()).orThrow
        } catch (e: Exception) {
            this.logger.error("Failed to read CustomNameTag config, generating default", e)
            this.config = NametagConfig()
        }
        this.writeConfig(server)
    }

    fun writeConfig(server: MinecraftServer) {
        val config = this.configPath.resolve("config.json")
        try {
            config.createParentDirectories()
            JsonUtils.encodeWith(this.config, NametagConfig.CODEC, config, server.registryAccess())
        } catch (e: Exception) {
            this.logger.error("Failed to write CustomNameTag config", e)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun migrateOldConfigs() {
        val oldPath = this.configPath.resolveSibling("CustomNameTags")
        try {
            if (oldPath.isDirectory()) {
                oldPath.copyToRecursively(this.configPath, overwrite = false, followLinks = true)
                oldPath.deleteRecursively()
            }
        } catch (_: IOException) {
            this.logger.error("Failed to migrate CustomNameTag configs!")
        }
    }
}
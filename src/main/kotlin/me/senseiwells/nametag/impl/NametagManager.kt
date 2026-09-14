package me.senseiwells.nametag.impl

import eu.pb4.predicate.api.PredicateRegistry
import me.senseiwells.config.JsonConfigFile
import me.senseiwells.nametag.CustomNameTags
import me.senseiwells.nametag.impl.config.LegacyConfigFixerUpper
import me.senseiwells.nametag.impl.config.NametagConfig
import me.senseiwells.nametag.impl.config.NametagDefinition
import me.senseiwells.nametag.impl.config.NametagOverride
import net.casual.arcade.nametags.extensions.EntityNametagExtension.Companion.nametagExtension
import net.casual.arcade.nametags.virtual.NametagHeight
import net.casual.arcade.utils.serialization.kotlin.CodecSerializersModule
import net.casual.arcade.utils.server.player
import net.casual.arcade.utils.server.players
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId

object NametagManager {
    private val path = CustomNameTags.configDir.resolve("config.json")

    private var file: JsonConfigFile<NametagConfig>? = null

    var config: NametagConfig = NametagConfig()
        private set

    fun load(server: MinecraftServer) {
        val file = this.createFile(server)
        this.file = file
        this.config = file.read()
    }

    fun save() {
        this.file?.write(this.config)
    }

    fun reload(server: MinecraftServer) {
        this.load(server)
        this.refreshAll(server)
    }

    fun resolve(player: ServerPlayer): List<ConfiguredNametag> {
        val overrides = this.config.getOverridesFor(player.uuid, player.gameProfile.name)
        val ids = LinkedHashSet(this.config.nametags.keys)
        ids.addAll(overrides.keys)

        val nametags = ArrayList<ConfiguredNametag>()
        for (id in ids) {
            val override = overrides[id]
            if (override != null && override.isDisabled) {
                continue
            }
            val definition = this.config.nametags[id] ?: NametagDefinition()
            val resolved = override?.applyTo(definition) ?: definition
            nametags.add(ConfiguredNametag(id, resolved))
        }
        return nametags.sortedByDescending { it.definition.priority }
    }

    fun refresh(player: ServerPlayer) {
        val extension = player.nametagExtension
        extension.removeAll()
        for (nametag in this.resolve(player)) {
            extension.add(nametag)
        }
    }

    fun refreshAll(server: MinecraftServer) {
        for (player in server.players) {
            this.refresh(player)
        }
    }

    fun refresh(server: MinecraftServer, profile: NameAndId) {
        val player = server.player(profile.id) ?: return
        this.refresh(player)
    }

    fun hasDefinition(id: String): Boolean {
        return this.config.nametags.containsKey(id)
    }

    fun getDefinition(id: String): NametagDefinition? {
        return this.config.nametags[id]
    }

    fun setDefinition(id: String, definition: NametagDefinition) {
        this.config.nametags[id] = definition
    }

    fun removeDefinition(id: String): NametagDefinition? {
        return this.config.nametags.remove(id)
    }

    fun getOverride(profile: NameAndId, id: String): NametagOverride? {
        return this.config.getOverridesFor(profile)?.get(id)
    }

    fun updateOverride(profile: NameAndId, id: String, updater: (NametagOverride) -> NametagOverride) {
        val overrides = this.config.getOrCreateOverridesFor(profile)
        overrides[id] = updater.invoke(overrides[id] ?: NametagOverride())
    }

    fun removeOverride(profile: NameAndId, id: String): NametagOverride? {
        val key = this.config.findPlayerKey(profile.id, profile.name) ?: return null
        val overrides = this.config.players[key] ?: return null
        val removed = overrides.remove(id)
        if (overrides.isEmpty()) {
            this.config.players.remove(key)
        }
        return removed
    }

    fun removeOverrides(profile: NameAndId): Map<String, NametagOverride>? {
        val key = this.config.findPlayerKey(profile.id, profile.name) ?: return null
        return this.config.players.remove(key)
    }

    private fun createFile(server: MinecraftServer): JsonConfigFile<NametagConfig> {
        return JsonConfigFile.create(
            this.path,
            ::NametagConfig,
            logger = CustomNameTags.logger,
            upgrades = listOf(LegacyConfigFixerUpper)
        ) {
            serializersModule = CodecSerializersModule(server.registryAccess()) {
                contextual(ComponentSerialization.CODEC)
                contextual(NametagHeight.CODEC)
                contextual(PredicateRegistry.CODEC)
            }
        }
    }
}

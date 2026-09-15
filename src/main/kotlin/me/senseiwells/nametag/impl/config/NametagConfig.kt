package me.senseiwells.nametag.impl.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.server.players.NameAndId
import java.util.*

@Serializable
data class NametagConfig(
    @SerialName("version")
    val version: Int = VERSION,
    @SerialName("nametags")
    val nametags: MutableMap<String, NametagDefinition> = LinkedHashMap(),
    @SerialName("players")
    val players: MutableMap<String, MutableMap<String, @Serializable(with = NametagOverride.Serializer::class) NametagOverride>> = LinkedHashMap()
) {
    fun findPlayerKey(uuid: UUID, name: String): String? {
        val dashed = uuid.toString()
        if (this.players.containsKey(dashed)) {
            return dashed
        }
        val undashed = dashed.replace("-", "")
        return this.players.keys.find { key ->
            key.equals(name, ignoreCase = true) || key.equals(undashed, ignoreCase = true)
        }
    }

    fun getOverridesFor(uuid: UUID, name: String): Map<String, NametagOverride> {
        val key = this.findPlayerKey(uuid, name) ?: return mapOf()
        return this.players[key] ?: mapOf()
    }

    fun getOverridesFor(profile: NameAndId): MutableMap<String, NametagOverride>? {
        val key = this.findPlayerKey(profile.id, profile.name)
        if (key != null) {
            return this.players[key]
        }
        return null
    }

    fun getOrCreateOverridesFor(profile: NameAndId): MutableMap<String, NametagOverride> {
        return this.getOverridesFor(profile) ?: this.players.getOrPut(profile.id.toString(), ::LinkedHashMap)
    }

    fun globalNametagIds(): Set<String> {
        return this.nametags.keys
    }

    fun allNametagIds(): Set<String> {
        val ids = LinkedHashSet(this.nametags.keys)
        for (overrides in this.players.values) {
            ids.addAll(overrides.keys)
        }
        return ids
    }

    companion object {
        const val VERSION = 2
    }
}

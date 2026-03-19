package me.senseiwells.nametag.impl.config

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.nametag.impl.PlaceholderNametag
import net.casual.arcade.utils.serialization.codec.associateBy
import net.casual.arcade.utils.serialization.codec.fieldOfAny
import net.minecraft.resources.Identifier

data class NametagConfig(
    val nametags: MutableMap<Identifier, PlaceholderNametag> = LinkedHashMap()
) {
    companion object {
        private val NAMETAGS_CODEC = PlaceholderNametag.CODEC.listOf().associateBy(PlaceholderNametag::id)

        val CODEC: Codec<NametagConfig> = RecordCodecBuilder.create { instance ->
            instance.group(
                NAMETAGS_CODEC.fieldOfAny("nametags", "name_tags").orElse(LinkedHashMap()).forGetter(NametagConfig::nametags)
            ).apply(instance) { NametagConfig(LinkedHashMap(it)) }
        }
    }
}
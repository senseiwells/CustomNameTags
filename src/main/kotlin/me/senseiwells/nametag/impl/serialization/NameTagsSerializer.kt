package me.senseiwells.nametag.impl.serialization

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import me.senseiwells.nametag.impl.PlaceholderNameTag
import net.minecraft.resources.ResourceLocation

@OptIn(ExperimentalSerializationApi::class)
object NameTagsSerializer: KSerializer<Object2ObjectLinkedOpenHashMap<ResourceLocation, PlaceholderNameTag>> {
    private val serializer = ListSerializer(serializer<PlaceholderNameTag>())

    override val descriptor: SerialDescriptor = SerialDescriptor("NameTagsSerializer", serializer.descriptor)

    override fun deserialize(decoder: Decoder): Object2ObjectLinkedOpenHashMap<ResourceLocation, PlaceholderNameTag> {
        val nametags = serializer.deserialize(decoder)
        val map = Object2ObjectLinkedOpenHashMap<ResourceLocation, PlaceholderNameTag>()
        for (nametag in nametags) {
            map[nametag.id] = nametag
        }
        return map
    }

    override fun serialize(encoder: Encoder, value: Object2ObjectLinkedOpenHashMap<ResourceLocation, PlaceholderNameTag>) {
        this.serializer.serialize(encoder, value.values.toList())
    }
}
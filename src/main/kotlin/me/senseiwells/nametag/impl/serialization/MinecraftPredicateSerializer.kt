package me.senseiwells.nametag.impl.serialization

import com.google.gson.GsonBuilder
import eu.pb4.predicate.api.GsonPredicateSerializer
import eu.pb4.predicate.api.MinecraftPredicate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.BuiltInRegistries

object MinecraftPredicateSerializer: KSerializer<MinecraftPredicate> {
    private val GSON = GsonBuilder()
        .disableHtmlEscaping()
        .registerTypeHierarchyAdapter(MinecraftPredicate::class.java, GsonPredicateSerializer.create(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)))
        .create()

    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): MinecraftPredicate {
        if (decoder !is JsonDecoder) {
            throw IllegalArgumentException("Can only deserialize MinecraftPredicates as JSON")
        }

        return GSON.fromJson(decoder.json.encodeToString(decoder.decodeJsonElement()), MinecraftPredicate::class.java)
    }

    override fun serialize(encoder: Encoder, value: MinecraftPredicate) {
        if (encoder !is JsonEncoder) {
            throw IllegalArgumentException("Can only deserialize MinecraftPredicates as JSON")
        }

        encoder.encodeJsonElement(encoder.json.parseToJsonElement(GSON.toJson(value)))
    }
}
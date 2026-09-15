package me.senseiwells.nametag.impl.config

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.casual.arcade.nametags.virtual.NametagHeight
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.PlainTextContents

@Serializable
data class NametagOverride(
    @SerialName("enabled")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val enabled: Boolean? = null,
    @Contextual
    @SerialName("text")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val text: Component? = null,
    @SerialName("update_interval")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val updateInterval: Int? = null,
    @SerialName("priority")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val priority: Int? = null,
    @SerialName("visible_radius")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val visibleRadius: Double? = null,
    @SerialName("hidden_radius")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val hiddenRadius: Double? = null,
    @SerialName("visible_through_walls")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val visibleThroughWalls: Boolean? = null,
    @SerialName("visible_when_invisible")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val visibleWhenInvisible: Boolean? = null,
    @SerialName("visible_with_passengers")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val visibleWithPassengers: Boolean? = null,
    @Contextual
    @SerialName("shift_height")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val shiftHeight: NametagHeight? = null,
    @SerialName("for")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val observees: PlayerSelector? = null,
    @SerialName("visible_to")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val observers: PlayerSelector? = null
) {
    val isDisabled: Boolean
        get() = this.enabled == false

    fun applyTo(definition: NametagDefinition): NametagDefinition {
        return NametagDefinition(
            this.text ?: definition.text,
            this.updateInterval ?: definition.updateInterval,
            this.priority ?: definition.priority,
            this.visibleRadius ?: definition.visibleRadius,
            this.hiddenRadius ?: definition.hiddenRadius,
            this.visibleThroughWalls ?: definition.visibleThroughWalls,
            this.visibleWhenInvisible ?: definition.visibleWhenInvisible,
            this.visibleWithPassengers ?: definition.visibleWithPassengers,
            this.shiftHeight ?: definition.shiftHeight,
            this.observees ?: PlayerSelector.Everyone,
            this.observers ?: definition.observers
        )
    }

    private fun isOnlyEnabled(): Boolean {
        return this.enabled != null && this == NametagOverride(enabled = this.enabled)
    }

    private fun isOnlyText(): Boolean {
        return this.text != null && this == NametagOverride(text = this.text)
    }

    object Serializer: KSerializer<NametagOverride> {
        override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

        override fun deserialize(decoder: Decoder): NametagOverride {
            val input = decoder as? JsonDecoder
                ?: throw SerializationException("Nametag overrides can only be decoded from json")
            return when (val element = input.decodeJsonElement()) {
                is JsonObject -> input.json.decodeFromJsonElement(serializer(), element)
                is JsonPrimitive -> when {
                    element.isString -> NametagOverride(text = Component.literal(element.content))
                    element.booleanOrNull != null -> NametagOverride(enabled = element.boolean)
                    else -> throw SerializationException("Expected a nametag override but found $element")
                }
                is JsonArray -> throw SerializationException("Expected a nametag override but found an array")
            }
        }

        override fun serialize(encoder: Encoder, value: NametagOverride) {
            val output = encoder as? JsonEncoder
                ?: throw SerializationException("Nametag overrides can only be encoded to json")
            val text = value.text
            val element = when {
                value.isOnlyEnabled() -> JsonPrimitive(value.enabled)
                value.isOnlyText() && text != null && isPlainText(text) -> JsonPrimitive(text.string)
                else -> output.json.encodeToJsonElement(serializer(), value)
            }
            output.encodeJsonElement(element)
        }

        private fun isPlainText(component: Component): Boolean {
            return component.siblings.isEmpty() && component.style.isEmpty && component.contents is PlainTextContents
        }
    }
}

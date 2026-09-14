package me.senseiwells.nametag.impl.config

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.casual.arcade.nametags.virtual.NametagHeight
import net.minecraft.network.chat.Component

@Serializable
data class NametagDefinition(
    @Contextual
    @SerialName("text")
    val text: Component = Component.empty(),
    @SerialName("update_interval")
    val updateInterval: Int = 1,
    @SerialName("priority")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val priority: Int = 0,
    @SerialName("visible_radius")
    val visibleRadius: Double = -1.0,
    @SerialName("hidden_radius")
    val hiddenRadius: Double = -1.0,
    @SerialName("visible_through_walls")
    val visibleThroughWalls: Boolean = true,
    @SerialName("visible_when_invisible")
    val visibleWhenInvisible: Boolean = false,
    @SerialName("visible_with_passengers")
    val visibleWithPassengers: Boolean = false,
    @Contextual
    @SerialName("shift_height")
    val shiftHeight: NametagHeight = NametagHeight.DEFAULT,
    @SerialName("for")
    val observees: PlayerSelector = PlayerSelector.Everyone,
    @SerialName("visible_to")
    val observers: PlayerSelector = PlayerSelector.Everyone
)

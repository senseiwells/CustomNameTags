package me.senseiwells.nametag.impl

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.node.TextNode
import eu.pb4.predicate.api.MinecraftPredicate
import eu.pb4.predicate.api.PredicateContext
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import me.senseiwells.nametag.api.NameTag
import me.senseiwells.nametag.impl.serialization.ComponentSerializer
import me.senseiwells.nametag.impl.serialization.MinecraftPredicateSerializer
import me.senseiwells.nametag.impl.serialization.ResourceLocationSerializer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity

@Serializable
@OptIn(ExperimentalSerializationApi::class)
class PlaceholderNameTag(
    @Serializable(with = ResourceLocationSerializer::class)
    val id: ResourceLocation,
    @JsonNames("literal")
    @Serializable(with = ComponentSerializer::class)
    val display: Component = Component.empty(),
    @SerialName("update_interval")
    override val updateInterval: Int = 1,
    @SerialName("visible_radius")
    val visibleRadius: Double = -1.0,
    @SerialName("visible_through_walls")
    @EncodeDefault(Mode.NEVER)
    override val visibleThroughWalls: Boolean = true,
    @SerialName("shift_height")
    @EncodeDefault(Mode.NEVER)
    val shiftHeight: ShiftHeight = ShiftHeight.SMALL,
    @EncodeDefault(Mode.NEVER)
    @SerialName("observee_predicate")
    @Serializable(with = MinecraftPredicateSerializer::class)
    val observee: MinecraftPredicate? = null,
    @EncodeDefault(Mode.NEVER)
    @SerialName("observer_predicate")
    @Serializable(with = MinecraftPredicateSerializer::class)
    val observer: MinecraftPredicate? = null
): NameTag {
    private val node: TextNode by lazy { Placeholders.parseNodes(TextNode.convert(this.display)) }

    override fun getComponent(entity: Entity): Component {
        return this.node.toText(PlaceholderContext.of(entity))
    }

    override fun getShift(): ShiftHeight {
        return this.shiftHeight
    }

    override fun isObservable(observee: Entity, observer: ServerPlayer): Boolean {
        var result = this.observee?.test(PredicateContext.of(observee))?.success ?: true
        result = result && (this.observer?.test(PredicateContext.of(observer))?.success ?: true)
        return result && this.isWithinRange(observee, observer)
    }

    private fun isWithinRange(observee: Entity, observer: ServerPlayer): Boolean {
        return this.visibleRadius < 0 || observee.distanceToSqr(observer) < (this.visibleRadius * this.visibleRadius)
    }
}
package me.senseiwells.nametag.impl

import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData
import eu.pb4.polymer.virtualentity.api.tracker.SimpleDataTracker
import kotlinx.serialization.Serializable
import me.senseiwells.nametag.impl.serialization.ShiftHeightSerializer
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType.ARMOR_STAND
import net.minecraft.world.entity.ai.attributes.AttributeInstance
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.Vec3
import java.util.*
import java.util.function.Consumer

@Serializable(with = ShiftHeightSerializer::class)
class ShiftHeight private constructor(internal val scale: Double) {
    private val changed: List<SynchedEntityData.DataValue<*>>?
    private var attribute: AttributeInstance? = null

    init {
        val tracker = SimpleDataTracker(ARMOR_STAND)
        tracker.set(EntityTrackedData.SILENT, true)
        tracker.set(EntityTrackedData.NO_GRAVITY, true)
        tracker.set(EntityTrackedData.FLAGS, (1 shl EntityTrackedData.INVISIBLE_FLAG_INDEX).toByte())

        this.changed = tracker.changedEntries

        if (this.scale != 1.0) {
            val attribute = AttributeInstance(Attributes.SCALE) { }
            val modifier = AttributeModifier(SCALE_IDENTIFIER, this.scale - 1.0, ADD_MULTIPLIED_BASE)
            attribute.addPermanentModifier(modifier)
            this.attribute = attribute
        }
    }

    fun applySpawnPackets(id: Int, uuid: UUID, position: Vec3, consumer: Consumer<Packet<ClientGamePacketListener>>) {
        consumer.accept(ClientboundAddEntityPacket(
            id, uuid, position.x, position.y, position.z, 0.0F, 0.0F, ARMOR_STAND, 0, Vec3.ZERO, 0.0
        ))

        if (this.changed != null) {
            consumer.accept(ClientboundSetEntityDataPacket(id, this.changed))
        }
        val attribute = this.attribute
        if (attribute != null) {
            consumer.accept(ClientboundUpdateAttributesPacket(id, listOf(attribute)))
        }
    }

    companion object {
        private val SCALE_IDENTIFIER = ResourceLocation.fromNamespaceAndPath("nametag", "scale")

        val SMALL = of(0.275)
        val DEFAULT = of(0.45)

        fun of(height: Double): ShiftHeight {
            return ShiftHeight(Mth.clamp(height / 1.975, 0.0625, 16.0))
        }
    }
}
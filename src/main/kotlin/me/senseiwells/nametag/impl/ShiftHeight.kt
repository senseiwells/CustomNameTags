package me.senseiwells.nametag.impl

import eu.pb4.polymer.virtualentity.api.tracker.DataTrackerLike
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData
import eu.pb4.polymer.virtualentity.api.tracker.SimpleDataTracker
import me.senseiwells.nametag.mixin.AgeableMobAccessor
import me.senseiwells.nametag.mixin.AreaEffectCloudAccessor
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType

enum class ShiftHeight(
    internal val type: EntityType<*>,
    modifier: DataTrackerLike.() -> Unit = { }
) {
    // 0.3 Blocks
    Medium(EntityType.BEE, {
        set(AgeableMobAccessor.getIsBabyDataAccessor(), true)
    }),
    // 0.5 Blocks
    MediumLarge(EntityType.AREA_EFFECT_CLOUD, {
        set(AreaEffectCloudAccessor.getRadiusAccessor(), 0.0F)
    }),
    // 0.6 Blocks
    Large(EntityType.BEE),
    // 0.9 Blocks
    ExtraLarge(EntityType.BAT);

    private val changed: List<SynchedEntityData.DataValue<*>>?

    init {
        val tracker = SimpleDataTracker(this.type)
        tracker.set(EntityTrackedData.SILENT, true)
        tracker.set(EntityTrackedData.NO_GRAVITY, true)
        tracker.set(EntityTrackedData.FLAGS, (1 shl EntityTrackedData.INVISIBLE_FLAG_INDEX).toByte())
        modifier(tracker)
        this.changed = tracker.changedEntries
    }

    fun createDataPacket(id: Int): ClientboundSetEntityDataPacket? {
        return ClientboundSetEntityDataPacket(id, this.changed ?: return null)
    }
}
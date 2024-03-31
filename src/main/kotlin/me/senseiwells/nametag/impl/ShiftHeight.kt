package me.senseiwells.nametag.impl

import eu.pb4.polymer.virtualentity.api.tracker.DataTrackerLike
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData
import eu.pb4.polymer.virtualentity.api.tracker.SimpleDataTracker
import me.senseiwells.nametag.mixin.AgeableMobAccessor
import me.senseiwells.nametag.mixin.AreaEffectCloudAccessor
import me.senseiwells.nametag.mixin.ArmorStandAccessor
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.EntityType

enum class ShiftHeight(
    internal val type: EntityType<*>,
    modifier: DataTrackerLike.() -> Unit = { }
) {
    /**
     * This is the default height for text displays.
     * Its height is equivalent to 0.3 blocks.
     */
    Medium(EntityType.BEE, {
        set(AgeableMobAccessor.getIsBabyDataAccessor(), true)
    }),

    /**
     * This is a slightly larger height for text displays.
     * It is used as the initial shift above the player's head.
     * Its height is equivalent to 0.5 blocks.
     */
    MediumLarge(EntityType.AREA_EFFECT_CLOUD, {
        set(AreaEffectCloudAccessor.getRadiusAccessor(), 0.0F)
    }),

    /**
     * This height is equivalent to 0.6 blocks.
     */
    Large(EntityType.BEE),

    /**
     * This height is equivalent to 0.9 blocks.
     */
    ExtraLarge(EntityType.BAT),

    /**
     * This height is equivalent to 0.9875 blocks.
     */
    ExtraExtraLarge(EntityType.ARMOR_STAND, {
        set(ArmorStandAccessor.getClientFlagsAccessor(), 1)
    });

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
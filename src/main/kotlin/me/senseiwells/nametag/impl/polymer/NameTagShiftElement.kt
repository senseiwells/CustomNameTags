package me.senseiwells.nametag.impl.polymer

import eu.pb4.polymer.virtualentity.api.elements.GenericEntityElement
import me.senseiwells.nametag.mixin.AgeableMobAccessor
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3

class NameTagShiftElement: GenericEntityElement() {
    init {
        this.dataTracker.set(AgeableMobAccessor.getIsBabyDataAccessor(), true)
        this.setNoGravity(true)
        this.isSilent = true

        this.ignorePositionUpdates()
    }

    override fun getEntityType(): EntityType<out Entity> {
        return EntityType.BEE
    }

    override fun createSpawnPacket(player: ServerPlayer): Packet<ClientGamePacketListener> {
        // We don't want super#createSpawnPacket because it calls this.holder which may be null...
        return ClientboundAddEntityPacket(
            this.entityId,
            this.uuid,
            player.x,
            player.y,
            player.z,
            this.pitch,
            this.yaw,
            EntityType.BEE,
            0,
            Vec3.ZERO,
            yaw.toDouble()
        )
    }
}
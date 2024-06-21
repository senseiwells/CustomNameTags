package me.senseiwells.nametag.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientboundAddEntityPacket.class)
public class ClientboundAddEntityPacketMixin {
	@ModifyExpressionValue(
		method = "<init>(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerEntity;I)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerEntity;getPositionBase()Lnet/minecraft/world/phys/Vec3;"
		)
	)
	private static Vec3 modifyEntityPosition(Vec3 original, Entity entity) {
		// Basically mojang is very silly, and they don't send the
		// actual position of the entity, and this breaks stuff
		return entity.position();
	}
}

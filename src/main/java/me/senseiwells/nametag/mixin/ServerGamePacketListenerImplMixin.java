package me.senseiwells.nametag.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Cancellable;
import me.senseiwells.nametag.NameTagHolderExtension;
import me.senseiwells.nametag.impl.entity.NameTagHolder;
import me.senseiwells.nametag.impl.entity.NameTagInteractionBypass;
import me.senseiwells.nametag.impl.entity.NameTagShiftElement;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements NameTagHolderExtension {
	@Unique private final NameTagHolder nametag$holder = new NameTagHolder(() -> this.player);

	@Shadow public ServerPlayer player;

	@Unique
	@Override
	public NameTagHolder nametag$getHolder() {
		return this.nametag$holder;
	}

	@ModifyExpressionValue(
		method = "handleInteract",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;getTarget(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"
		)
	)
	private Entity checkValidTarget(
		Entity original,
		ServerboundInteractPacket packet,
		@Cancellable CallbackInfo ci
	) {
		int id = ((ServerboundInteractionPacketAccessor) packet).getEntityId();
		if (NameTagShiftElement.isNameTagShift(id)) {
			packet.dispatch(new NameTagInteractionBypass(this.player));
			ci.cancel();
		}
		return original;
	}
}

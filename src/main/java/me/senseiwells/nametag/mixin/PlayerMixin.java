package me.senseiwells.nametag.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import me.senseiwells.nametag.impl.NameTagUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
	protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
		super(entityType, level);
	}

	@Inject(
		method = "updatePlayerPose",
		at = @At("HEAD")
	)
	private void beforeUpdatePlayerPose(CallbackInfo ci, @Share("previous") LocalRef<Pose> pose) {
		pose.set(this.getPose());
	}

	@Inject(
		method = "updatePlayerPose",
		at = @At("TAIL")
	)
	private void afterUpdatePlayerPose(CallbackInfo ci, @Share("previous") LocalRef<Pose> pose) {
		if ((Object) this instanceof ServerPlayer player) {
			Pose previous = pose.get();
			Pose current = this.getPose();
			if (previous != current) {
				if (previous == Pose.CROUCHING) {
					NameTagUtils.unsneakNameTags(player);
				} else if (current == Pose.CROUCHING) {
					NameTagUtils.sneakNameTags(player);
				}
			}
		}
	}
}

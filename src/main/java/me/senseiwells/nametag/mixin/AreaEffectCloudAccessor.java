package me.senseiwells.nametag.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AreaEffectCloud.class)
public interface AreaEffectCloudAccessor {
	@Accessor("DATA_RADIUS")
	static EntityDataAccessor<Float> getRadiusAccessor() {
		throw new AssertionError();
	}
}

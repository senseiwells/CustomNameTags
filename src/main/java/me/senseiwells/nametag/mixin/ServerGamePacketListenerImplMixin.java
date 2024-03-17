package me.senseiwells.nametag.mixin;

import me.senseiwells.nametag.NameTagHolderExtension;
import me.senseiwells.nametag.impl.entity.NameTagHolder;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements NameTagHolderExtension {
	@Unique
	private final NameTagHolder nametag$holder = new NameTagHolder((ServerGamePacketListenerImpl) (Object) this);

	@Unique
	@Override
	public NameTagHolder nametag$getHolder() {
		return this.nametag$holder;
	}
}

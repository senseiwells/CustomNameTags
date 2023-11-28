package me.senseiwells.nametag.mixin;

import me.senseiwells.nametag.impl.NameTagExtension;
import me.senseiwells.nametag.ExtensionHolder;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements ExtensionHolder {
	@Unique
	private final NameTagExtension nametag$extension = new NameTagExtension((ServerGamePacketListenerImpl) (Object) this);

	@Unique
	@Override
	public NameTagExtension nametag$getExtension() {
		return this.nametag$extension;
	}
}

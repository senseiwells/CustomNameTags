package me.senseiwells.nametag;

import me.senseiwells.nametag.impl.entity.NameTagHolder;
import org.jetbrains.annotations.Nullable;

public interface NameTagHolderExtension {
	@SuppressWarnings("unused")
	@Nullable NameTagHolder nametag$getHolder();
}

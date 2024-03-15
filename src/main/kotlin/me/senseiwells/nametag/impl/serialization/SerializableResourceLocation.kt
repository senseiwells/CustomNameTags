package me.senseiwells.nametag.impl.serialization

import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

typealias SerializableResourceLocation = @Serializable(with = ResourceLocationSerializer::class) ResourceLocation
package me.senseiwells.nametag.impl.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.senseiwells.nametag.impl.ShiftHeight

object ShiftHeightSerializer: KSerializer<ShiftHeight> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ShiftHeight", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): ShiftHeight {
        return ShiftHeight.of(decoder.decodeDouble())
    }

    override fun serialize(encoder: Encoder, value: ShiftHeight) {
        encoder.encodeDouble(value.scale)
    }
}
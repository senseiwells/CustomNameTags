package me.senseiwells.nametag.impl.predicate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.predicate.api.AbstractPredicate
import eu.pb4.predicate.api.PredicateContext
import eu.pb4.predicate.api.PredicateResult
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import java.util.*

class UUIDPredicate(val uuid: UUID): AbstractPredicate(ID, CODEC) {
    override fun test(context: PredicateContext): PredicateResult<*> {
        val entity = context.entity() ?: return PredicateResult.ofFailure()
        return PredicateResult.ofBoolean(entity.uuid == this.uuid)
    }

    companion object {
        private val UUID_CODEC = Codec.withAlternative(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC)

        val ID: Identifier = Identifier.withDefaultNamespace("uuid")

        val CODEC: MapCodec<UUIDPredicate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                UUID_CODEC.fieldOf("uuid").forGetter(UUIDPredicate::uuid)
            ).apply(instance, ::UUIDPredicate)
        }
    }
}
package me.senseiwells.nametag.impl.predicate

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.predicate.api.AbstractPredicate
import eu.pb4.predicate.api.PredicateContext
import eu.pb4.predicate.api.PredicateResult
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.ResourceLocation
import java.util.*

class UUIDPredicate(val uuid: UUID): AbstractPredicate(ID, CODEC) {
    override fun test(context: PredicateContext): PredicateResult<*> {
        val entity = context.entity ?: return PredicateResult.ofFailure()
        return PredicateResult.ofBoolean(entity.uuid == this.uuid)
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.withDefaultNamespace("uuid")

        val CODEC: MapCodec<UUIDPredicate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                UUIDUtil.LENIENT_CODEC.fieldOf("uuid").forGetter(UUIDPredicate::uuid)
            ).apply(instance, ::UUIDPredicate)
        }
    }
}
package me.senseiwells.nametag.impl.predicate

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import eu.pb4.predicate.api.AbstractPredicate
import eu.pb4.predicate.api.PredicateContext
import eu.pb4.predicate.api.PredicateResult
import net.minecraft.resources.ResourceLocation

class ScoreboardTagPredicate(val tag: String): AbstractPredicate(ID, CODEC) {
    override fun test(context: PredicateContext): PredicateResult<*> {
        val entity = context.entity ?: return PredicateResult.ofFailure()
        return PredicateResult.ofBoolean(entity.tags.contains(this.tag))
    }

    companion object {
        val ID = ResourceLocation("scoreboard_tag")

        val CODEC: MapCodec<ScoreboardTagPredicate> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.STRING.fieldOf("value").forGetter(ScoreboardTagPredicate::tag)
            ).apply(instance, ::ScoreboardTagPredicate)
        }
    }
}
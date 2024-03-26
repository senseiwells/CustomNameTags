package me.senseiwells.nametag.impl.predicate

import eu.pb4.predicate.api.PredicateRegistry

object ExtraPredicates {
    fun register() {
        PredicateRegistry.register(ScoreboardTagPredicate.ID, ScoreboardTagPredicate.CODEC)
    }
}
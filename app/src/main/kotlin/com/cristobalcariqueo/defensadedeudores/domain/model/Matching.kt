package com.cristobalcariqueo.defensadedeudores.domain.model

/**
 * A retReg match proposal -- always suggest-and-confirm, never applied
 * silently (SCOPE.md). Forward direction fires on new normal reg creation;
 * retro direction fires right after a retReg is created.
 */
sealed interface MatchSuggestion {
    /** Forward case 1: [retReg] fully covers [newReg] -> newReg checked + linked, retReg reduced. */
    data class NewRegCovered(val newReg: Registry, val retReg: Registry) : MatchSuggestion

    /** Forward case 2: [retReg] partially covers [newReg] -> newReg reduced, retReg consumed. */
    data class NewRegReduced(val newReg: Registry, val retReg: Registry) : MatchSuggestion

    /** Retro case 1: [retReg] fully covers [normal] -> normal checked + linked, retReg reduced. */
    data class ExistingCovered(val retReg: Registry, val normal: Registry) : MatchSuggestion

    /** Retro case 2: [retReg] partially covers [normal] -> normal reduced, retReg consumed. */
    data class ExistingReduced(val retReg: Registry, val normal: Registry) : MatchSuggestion
}

/** Closest-fit candidate selection (SCOPE.md match rules). Pure -- unit-tested. */
object RetRegMatcher {

    /**
     * New normal reg vs the debtor's unchecked retRegs:
     * 1. smallest retReg with amount >= the reg's -> full cover;
     * 2. else largest retReg with amount < the reg's -> partial cover;
     * 3. else null (no suggestion).
     */
    fun forward(newReg: Registry, uncheckedRetRegs: List<Registry>): MatchSuggestion? {
        val candidates = uncheckedRetRegs.filter { it.type == RegistryType.RETURN && !it.checked }
        return candidates.filter { it.amount >= newReg.amount }.minByOrNull { it.amount }
            ?.let { MatchSuggestion.NewRegCovered(newReg, it) }
            ?: candidates.filter { it.amount < newReg.amount }.maxByOrNull { it.amount }
                ?.let { MatchSuggestion.NewRegReduced(newReg, it) }
    }

    /**
     * Fresh retReg vs the debtor's existing unchecked normal regs (mirror of
     * [forward]): 1. largest normal <= the retReg's amount -> full cover;
     * 2. else smallest normal > it -> partial cover; 3. else null.
     */
    fun retro(retReg: Registry, uncheckedNormals: List<Registry>): MatchSuggestion? {
        if (retReg.checked || retReg.amount <= 0) return null
        val candidates = uncheckedNormals.filter { it.type == RegistryType.NORMAL && !it.checked }
        return candidates.filter { it.amount <= retReg.amount }.maxByOrNull { it.amount }
            ?.let { MatchSuggestion.ExistingCovered(retReg, it) }
            ?: candidates.filter { it.amount > retReg.amount }.minByOrNull { it.amount }
                ?.let { MatchSuggestion.ExistingReduced(retReg, it) }
    }
}

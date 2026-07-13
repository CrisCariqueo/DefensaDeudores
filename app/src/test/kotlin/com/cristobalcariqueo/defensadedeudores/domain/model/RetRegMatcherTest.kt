package com.cristobalcariqueo.defensadedeudores.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetRegMatcherTest {

    private fun reg(
        id: String,
        amount: Int,
        type: RegistryType,
        checked: Boolean = false,
    ) = Registry(
        id = id,
        trackId = "t",
        personId = "p",
        sourceId = if (type == RegistryType.NORMAL) "s" else null,
        amount = amount,
        type = type,
        checked = checked,
        note = null,
        date = LocalDate(2026, 7, 12),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun normal(id: String, amount: Int, checked: Boolean = false) =
        reg(id, amount, RegistryType.NORMAL, checked)

    private fun retReg(id: String, amount: Int, checked: Boolean = false) =
        reg(id, amount, RegistryType.RETURN, checked)

    // ---- forward: new normal reg vs unchecked retRegs

    @Test
    fun `forward picks smallest retReg that covers the new reg`() {
        val suggestion = RetRegMatcher.forward(
            normal("n", 5_000),
            listOf(retReg("r1", 20_000), retReg("r2", 6_000), retReg("r3", 5_000)),
        )
        assertEquals(
            "r3",
            (suggestion as MatchSuggestion.NewRegCovered).retReg.id,
        )
    }

    @Test
    fun `forward falls back to largest retReg below the new reg`() {
        val suggestion = RetRegMatcher.forward(
            normal("n", 10_000),
            listOf(retReg("r1", 2_000), retReg("r2", 7_000)),
        )
        assertEquals(
            "r2",
            (suggestion as MatchSuggestion.NewRegReduced).retReg.id,
        )
    }

    @Test
    fun `forward ignores checked retRegs and returns null when none apply`() {
        assertNull(RetRegMatcher.forward(normal("n", 10_000), emptyList()))
        assertNull(
            RetRegMatcher.forward(
                normal("n", 10_000),
                listOf(retReg("r1", 20_000, checked = true)),
            ),
        )
    }

    // ---- retro: fresh retReg vs unchecked normal regs

    @Test
    fun `retro picks largest normal the retReg fully covers`() {
        val suggestion = RetRegMatcher.retro(
            retReg("r", 10_000),
            listOf(normal("n1", 3_000), normal("n2", 9_000), normal("n3", 12_000)),
        )
        assertEquals(
            "n2",
            (suggestion as MatchSuggestion.ExistingCovered).normal.id,
        )
    }

    @Test
    fun `retro falls back to smallest normal above the retReg amount`() {
        val suggestion = RetRegMatcher.retro(
            retReg("r", 4_000),
            listOf(normal("n1", 12_000), normal("n2", 6_000)),
        )
        assertEquals(
            "n2",
            (suggestion as MatchSuggestion.ExistingReduced).normal.id,
        )
    }

    @Test
    fun `retro returns null for consumed or empty retRegs`() {
        assertNull(RetRegMatcher.retro(retReg("r", 0), listOf(normal("n1", 1_000))))
        assertNull(RetRegMatcher.retro(retReg("r", 5_000, checked = true), listOf(normal("n1", 1_000))))
        assertNull(RetRegMatcher.retro(retReg("r", 5_000), emptyList()))
    }

    @Test
    fun `exact cover prefers the equal amount and full-cover branch`() {
        val suggestion = RetRegMatcher.forward(
            normal("n", 5_000),
            listOf(retReg("r1", 5_000), retReg("r2", 4_999)),
        )
        assertEquals(
            "r1",
            (suggestion as MatchSuggestion.NewRegCovered).retReg.id,
        )
    }
}

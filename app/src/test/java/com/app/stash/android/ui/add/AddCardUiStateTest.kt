package com.app.stash.android.ui.add

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddCardUiStateTest {

    @Test
    fun `requires four numeric digits for last4`() {
        assertFalse(state(last4 = "", creditLimit = "1000").canSave)
        assertFalse(state(last4 = "123", creditLimit = "1000").canSave)
        assertFalse(state(last4 = "abcd", creditLimit = "1000").canSave)
        assertTrue(state(last4 = "4242", creditLimit = "1000").canSave)
    }

    @Test
    fun `requires parseable credit limit`() {
        assertFalse(state(last4 = "4242", creditLimit = "").canSave)
        assertFalse(state(last4 = "4242", creditLimit = "abc").canSave)
        assertTrue(state(last4 = "4242", creditLimit = "10000").canSave)
        assertTrue(state(last4 = "4242", creditLimit = "1234.56").canSave)
    }

    @Test
    fun `nickname and balance are optional`() {
        assertTrue(state(last4 = "4242", creditLimit = "5000").canSave)
        assertTrue(
            state(last4 = "4242", creditLimit = "5000", balance = "", nickname = "").canSave
        )
    }

    private fun state(
        last4: String = "",
        creditLimit: String = "",
        balance: String = "",
        nickname: String = ""
    ) = AddCardUiState(
        nickname = nickname,
        last4 = last4,
        creditLimit = creditLimit,
        balance = balance
    )
}

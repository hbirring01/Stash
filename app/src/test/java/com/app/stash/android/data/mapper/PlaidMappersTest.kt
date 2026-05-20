package com.app.stash.android.data.mapper

import com.app.stash.android.data.plaid.LiabilitiesGetResponse
import com.app.stash.android.data.plaid.PlaidAccount
import com.app.stash.android.data.plaid.PlaidBalances
import com.app.stash.android.data.plaid.PlaidCreditLiability
import com.app.stash.android.data.plaid.PlaidLiabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaidMappersTest {

    @Test
    fun `maps only credit accounts and resolves brand from name`() {
        val resp = LiabilitiesGetResponse(
            accounts = listOf(
                account("acc-visa", "Visa Platinum", mask = "4242", limit = 10_000.0, current = 1_250.0),
                account("acc-amex", "American Express Gold", mask = "1009", limit = 25_000.0, current = 0.0),
                account("acc-checking", "Checking", mask = "0001", type = "depository")
            ),
            liabilities = PlaidLiabilities(
                credit = listOf(
                    PlaidCreditLiability(accountId = "acc-visa"),
                    PlaidCreditLiability(accountId = "acc-amex")
                )
            )
        )

        val entities = resp.toCreditCardEntities("item-1")

        assertEquals(2, entities.size)
        val visa = entities.first { it.sourceAccountId == "acc-visa" }
        assertEquals("Visa", visa.brand)
        assertEquals("4242", visa.last4)
        assertEquals(10_000.0, visa.creditLimit, 0.001)
        assertEquals(1_250.0, visa.balance, 0.001)
        assertEquals("item-1", visa.sourceItemId)

        val amex = entities.first { it.sourceAccountId == "acc-amex" }
        assertEquals("Amex", amex.brand)

        assertTrue(entities.none { it.sourceAccountId == "acc-checking" })
    }

    private fun account(
        id: String,
        name: String,
        mask: String? = null,
        limit: Double? = null,
        current: Double? = null,
        type: String? = "credit"
    ) = PlaidAccount(
        accountId = id,
        name = name,
        mask = mask,
        balances = PlaidBalances(current = current, limit = limit),
        type = type
    )
}

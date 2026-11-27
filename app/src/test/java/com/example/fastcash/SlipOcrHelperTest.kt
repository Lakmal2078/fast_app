package com.example.fastcash

import com.example.fastcash.data.model.DefaultAgentAccounts
import com.example.fastcash.ui.components.SlipOcrHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlipOcrHelperTest {

    @Test
    fun parsesPeoplesBankSlipWithLkrAmountAndRef() {
        val sampleText = """
            People's Bank Fund Transfer
            Beneficiary Account: 120200380030196
            Beneficiary Name: VGS Lakmal
            Transfer Amount: LKR 25,000.00
            Reference No: FT26112800912
            Date: 28/11/2026 05:15 PM
        """.trimIndent()

        val result = SlipOcrHelper.parseOcrText(
            rawText = sampleText,
            agentAccounts = DefaultAgentAccounts.accounts,
            minAmount = 1000L,
            maxAmount = 500000L
        )

        assertEquals(25000L, result.detectedAmount)
        assertEquals("pb_walasmulla", result.detectedAccount?.id)
        assertEquals("Bank transfer", result.detectedMethod)
        assertEquals("FT26112800912", result.detectedReference)
    }

    @Test
    fun parsesIpayScreenshotIgnoringPhoneNumberAsAmount() {
        val sampleText = """
            iPay Payment Successful
            Paid To: 0740452530
            Amount: Rs. 5,000.00
            TRX ID: IPAY994812
        """.trimIndent()

        val result = SlipOcrHelper.parseOcrText(
            rawText = sampleText,
            agentAccounts = DefaultAgentAccounts.accounts,
            minAmount = 1000L,
            maxAmount = 500000L
        )

        assertEquals(5000L, result.detectedAmount)
        assertEquals("ipay_1", result.detectedAccount?.id)
        assertEquals("iPay wallet", result.detectedMethod)
        assertEquals("IPAY994812", result.detectedReference)
    }

    @Test
    fun ignoresOutOfRangeAmountsAndEmptyText() {
        val result = SlipOcrHelper.parseOcrText(
            rawText = "Unclear photo\nBalance LKR 250.00",
            agentAccounts = DefaultAgentAccounts.accounts,
            minAmount = 1000L,
            maxAmount = 500000L
        )

        assertNull(result.detectedAmount)
        assertNull(result.detectedAccount)
    }
}

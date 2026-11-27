package com.example.fastcash

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomerJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun submitInSinhalaAndTrackSavedRequest() {
        val key = requireNotNull(InstrumentationRegistry.getArguments().getString("stagingKey"))
        ui.onNodeWithTag("access_key").performTextInput(key)
        ui.onNodeWithTag("sign_in").performClick()
        waitForTag("lang_btn_SI")
        ui.onNodeWithTag("lang_btn_SI").performClick()
        ui.onNodeWithTag("nav_item_deposit").performClick()
        ui.onNodeWithText("ඔබේ ගිණුමට මුදල් එක් කරන්න").assertExists()
        val list = ui.onNode(hasScrollToIndexAction())
        fun field(tag: String, value: String) {
            list.performScrollToNode(hasTestTag(tag))
            ui.onNodeWithTag(tag).performTextInput(value)
        }
        field("deposit_amount_input", "1300")
        field("deposit_player_id_input", "TEST0001")
        field("deposit_receipt_ref_input", "STAGING-ONLY")
        list.performScrollToNode(hasTestTag("deposit_submit_btn"))
        ui.onNodeWithTag("deposit_submit_btn").performClick()
        waitForTag("deposit_done_btn")
        val ref = ui.onNode(hasText("TXN-", substring = true)).fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text].single().text
        assertTrue(ref.startsWith("TXN-"))
        ui.onNodeWithTag("deposit_done_btn").performClick()
        ui.waitUntil(30000) { ui.onAllNodesWithText("ඉල්ලීම හමු වුණා").fetchSemanticsNodes().isNotEmpty() }
        ui.onAllNodesWithText(ref).onFirst().assertExists()
    }

    private fun waitForTag(tag: String) {
        ui.waitUntil(30000) { ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }
}

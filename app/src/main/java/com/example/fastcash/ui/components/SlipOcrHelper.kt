package com.example.fastcash.ui.components

import android.graphics.BitmapFactory
import android.util.Log
import com.example.fastcash.data.model.AgentDestinationAccount
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SlipOcrResult(
    val detectedAmount: Long? = null,
    val detectedAccount: AgentDestinationAccount? = null,
    val detectedMethod: String? = null,
    val detectedReference: String? = null,
    val transactionDate: String? = null,
    val paymentStatus: String? = null,
    val transferType: String? = null,
    val sender: String? = null,
    val senderBank: String? = null,
    val senderAccount: String? = null,
    val recipientBank: String? = null,
    val recipientAccount: String? = null,
    val recipientName: String? = null,
    val remarks: String? = null
) {
    val hasAnyMatch: Boolean
        get() = detectedAmount != null || detectedAccount != null || detectedMethod != null || detectedReference != null ||
            transactionDate != null || paymentStatus != null || sender != null || recipientName != null
}

object SlipOcrHelper {
    private const val TAG = "SlipOcrHelper"

    private val CURRENCY_LINE_KEYWORDS = listOf(
        "lkr", "rs", "amount", "transfer", "total", "paid", "sent", "value", "deposit", "credit"
    )
    private val REFERENCE_PATTERNS = listOf(
        Regex("""\b(FT[A-Z0-9]{5,18})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:REF(?:ERENCE)?|TRX|TXN|SLIP|TRACE|RECEIPT)\s*(?:NO|ID|#|:|-)?\s*[:#-]?\s*([A-Z0-9-]{5,24})\b""", RegexOption.IGNORE_CASE)
    )
    private val AMOUNT_PATTERN = Regex(
        """(?i)(?:(?:LKR|Rs\.?)\s*)?(\d{1,3}(?:,\d{3})+|\d{4,6})(?:\.(\d{2}))?\b"""
    )

    suspend fun extractDepositDetails(
        jpegBytes: ByteArray,
        agentAccounts: List<AgentDestinationAccount>,
        minAmount: Long,
        maxAmount: Long
    ): SlipOcrResult = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return@withContext SlipOcrResult()
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            val fullText = visionText.text
            if (fullText.isBlank()) {
                return@withContext SlipOcrResult()
            }
            parseOcrText(
                rawText = fullText,
                agentAccounts = agentAccounts,
                minAmount = minAmount,
                maxAmount = maxAmount
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "On-device slip OCR scan failed", e)
            SlipOcrResult()
        } finally {
            runCatching { recognizer.close() }
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    internal fun parseOcrText(
        rawText: String,
        agentAccounts: List<AgentDestinationAccount>,
        minAmount: Long,
        maxAmount: Long
    ): SlipOcrResult {
        val normalizedLower = rawText.lowercase()
        val digitsOnly = rawText.filter { it.isDigit() }

        val matchedAccount = matchDestinationAccount(normalizedLower, digitsOnly, agentAccounts)
        val matchedMethod = when {
            matchedAccount != null -> matchedAccount.method
            normalizedLower.contains("ipay") -> "iPay wallet"
            normalizedLower.contains("cdm") || normalizedLower.contains("cash deposit") -> "Cash deposit"
            else -> null
        }

        val matchedAmount = extractBestAmount(
            rawText = rawText,
            agentAccounts = agentAccounts,
            minAmount = minAmount,
            maxAmount = maxAmount
        )

        val matchedReference = extractReference(rawText)

        return SlipOcrResult(
            detectedAmount = matchedAmount,
            detectedAccount = matchedAccount,
            detectedMethod = matchedMethod,
            detectedReference = matchedReference,
            transactionDate = labeledValue(rawText, "transaction date", "date & time", "date/time", "date", "time"),
            paymentStatus = labeledValue(rawText, "transaction status", "status")
                ?: if (Regex("(?i)\\b(successful|success|completed)\\b").containsMatchIn(rawText)) "SUCCESS (slip text)" else null,
            transferType = labeledValue(rawText, "transaction type", "type"),
            sender = labeledValue(rawText, "from", "sender name", "sender"),
            senderBank = labeledValue(rawText, "from bank", "sender bank"),
            senderAccount = labeledValue(rawText, "from account", "sender account"),
            recipientBank = labeledValue(rawText, "to bank", "beneficiary bank", "recipient bank"),
            recipientAccount = labeledValue(rawText, "to account", "beneficiary account", "recipient account"),
            recipientName = labeledValue(rawText, "recipient name", "beneficiary name", "account holder"),
            remarks = labeledValue(rawText, "remarks", "remark", "description", "narration")
        )
    }

    private fun labeledValue(rawText: String, vararg labels: String): String? {
        // Match explicit labels only; never invent transaction or settlement details.
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (label in labels) {
            val regex = Regex("(?i)^" + Regex.escape(label) + "\\s*[:#-]\\s*(.+)$")
            for (line in lines) {
                val candidate = regex.find(line)?.groupValues?.getOrNull(1)?.trim()?.take(140)
                if (!candidate.isNullOrBlank() && candidate != "-") return candidate
            }
        }
        return null
    }

    private fun matchDestinationAccount(
        lowerText: String,
        digitsOnly: String,
        agentAccounts: List<AgentDestinationAccount>
    ): AgentDestinationAccount? {
        // 1. Exact account number match (most reliable)
        agentAccounts.firstOrNull { acc ->
            val cleanNum = acc.accountNumber.filter { it.isDigit() }
            cleanNum.length >= 6 && digitsOnly.contains(cleanNum)
        }?.let { return it }

        // 2. Partial trailing number for iPay numbers (without leading 0)
        agentAccounts.filter { it.method == "iPay wallet" }.firstOrNull { acc ->
            val noLeadingZero = acc.accountNumber.trimStart('0')
            noLeadingZero.length >= 8 && digitsOnly.contains(noLeadingZero)
        }?.let { return it }

        // 3. Bank / account holder keyword heuristics
        return when {
            lowerText.contains("people") || lowerText.contains("walasmulla") && lowerText.contains("lakmal") ->
                agentAccounts.firstOrNull { it.id == "pb_walasmulla" }
            lowerText.contains("sampath") || lowerText.contains("neluwa") || lowerText.contains("oshadhi") ->
                agentAccounts.firstOrNull { it.id == "sampath_neluwa" }
            lowerText.contains("bank of ceylon") || Regex("""\bboc\b""").containsMatchIn(lowerText) ->
                agentAccounts.firstOrNull { it.id == "boc_head_office" }
            lowerText.contains("ipay") ->
                agentAccounts.firstOrNull { it.id == "ipay_1" }
            else -> null
        }
    }

    private fun extractBestAmount(
        rawText: String,
        agentAccounts: List<AgentDestinationAccount>,
        minAmount: Long,
        maxAmount: Long
    ): Long? {
        val knownAccountSubstrings = agentAccounts.map { it.accountNumber.filter(Char::isDigit) }
        val scoredCandidates = mutableListOf<Pair<Int, Long>>()

        rawText.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEach
            val lower = trimmed.lowercase()

            // Skip obvious phone/account/date metadata lines unless they explicitly carry currency
            val hasCurrencyMarker = lower.contains("lkr") || lower.contains("rs")
            if (!hasCurrencyMarker && (lower.contains("a/c") || lower.contains("acc") || lower.contains("date") || lower.contains("time"))) {
                return@forEach
            }

            AMOUNT_PATTERN.findAll(trimmed).forEach { match ->
                val fullMatch = match.value
                val integerPart = match.groupValues[1].replace(",", "")
                val parsed = integerPart.toLongOrNull() ?: return@forEach
                if (parsed !in minAmount..maxAmount) return@forEach

                // Avoid mistaking a fragment of a known bank/iPay number or phone number for an amount
                if (knownAccountSubstrings.any { it.contains(integerPart) && !fullMatch.contains(',') && !fullMatch.contains('.') }) {
                    return@forEach
                }
                if (integerPart.startsWith("07") || integerPart.startsWith("947")) {
                    return@forEach
                }

                var score = 1
                if (fullMatch.contains("lkr", ignoreCase = true) || fullMatch.contains("rs", ignoreCase = true)) {
                    score += 6
                }
                if (match.groupValues[2].isNotEmpty()) {
                    score += 4 // .00 decimals strongly indicate a monetary figure
                }
                if (fullMatch.contains(",")) {
                    score += 2
                }
                if (CURRENCY_LINE_KEYWORDS.any { lower.contains(it) }) {
                    score += 3
                }
                // Round numbers ending in 00 are very common in LKR transfers
                if (parsed % 100L == 0L) {
                    score += 1
                }
                scoredCandidates.add(score to parsed)
            }
        }

        return scoredCandidates.maxByOrNull { it.first }?.second
    }

    private fun extractReference(rawText: String): String? {
        for (pattern in REFERENCE_PATTERNS) {
            val match = pattern.find(rawText) ?: continue
            val candidate = match.groupValues.getOrNull(1)?.trim()?.take(64)
            if (!candidate.isNullOrBlank() && candidate.any(Char::isDigit)) {
                return candidate.uppercase()
            }
        }
        return null
    }
}

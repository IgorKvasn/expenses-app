package com.example.expensetracker.domain.usecase

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class ExtractReceiptAmountUseCase @Inject constructor() {

    suspend fun execute(bitmap: Bitmap): Result<String> {
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            val fullText = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { result -> continuation.resume(result.text) }
                    .addOnFailureListener { e -> continuation.cancel(e) }
            }
            val amount = parseTotal(fullText)
            if (amount != null) {
                Result.success(amount)
            } else {
                Result.failure(Exception("Could not find total amount on receipt"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun parseTotal(text: String): String? {
            val lines = text.lines().map { it.trim() }

            // Strategy 1: Look for "NA ÚHRADU" line (Slovak receipts) — the amount
            // is either on the same line or on the next line
            for ((index, line) in lines.withIndex()) {
                if (line.contains("ÚHRADU", ignoreCase = true) ||
                    line.contains("UHRADU", ignoreCase = true)
                ) {
                    extractAmount(line)?.let { return it }
                    if (index + 1 < lines.size) {
                        extractAmount(lines[index + 1])?.let { return it }
                    }
                }
            }

            // Strategy 2: Look for common total labels
            val totalPatterns = listOf(
                "TOTAL", "SUMME", "CELKEM", "SPOLU", "SUMA", "ZUSAMMEN",
                "ÖSSZESEN", "RAZEM", "ИТОГО",
            )
            for ((index, line) in lines.withIndex()) {
                val upperLine = line.uppercase()
                if (totalPatterns.any { upperLine.contains(it) }) {
                    extractAmount(line)?.let { return it }
                    if (index + 1 < lines.size) {
                        extractAmount(lines[index + 1])?.let { return it }
                    }
                }
            }

            // Strategy 3: Look for the largest amount on the receipt as a fallback
            val allAmounts = lines.mapNotNull { extractAmount(it)?.toDoubleOrNull() }
            val largest = allAmounts.maxOrNull()
            if (largest != null) {
                return formatAmount(largest)
            }

            return null
        }

        private fun extractAmount(line: String): String? {
            // Match amounts like "9,19" or "9.19" or "1 234,56" — the last number on the line
            val regex = Regex("""(\d[\d\s]*[.,]\d{2})\b""")
            val matches = regex.findAll(line).toList()
            val match = matches.lastOrNull() ?: return null
            val raw = match.groupValues[1]
                .replace(" ", "")
                .replace(",", ".")
            val value = raw.toDoubleOrNull() ?: return null
            if (value <= 0) return null
            return formatAmount(value)
        }

        private fun formatAmount(value: Double): String {
            val cents = (value * 100).toLong()
            val euros = cents / 100
            val remainingCents = cents % 100
            return if (remainingCents == 0L) "$euros" else "$euros.${"%02d".format(remainingCents)}"
        }
    }
}

package com.example.expensetracker.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractReceiptAmountUseCaseTest {

    @Test
    fun `parses Slovak Lidl receipt with NA UHRADU`() {
        val text = """
            Farm.kur.prs.rezne
            0,961 kg * 8,99     8,64 C
            Tvar.tyč. proteín 1 ks   0,55 B
            MEDZISÚČET          9,19
            NA ÚHRADU EUR       9,19
            KARTA               9,19
        """.trimIndent()
        assertEquals("9.19", ExtractReceiptAmountUseCase.parseTotal(text))
    }

    @Test
    fun `parses Slovak Kaufland receipt`() {
        val text = """
            K.Tvaroh polotu 250g
            2 ks * 0,79          1,58 F
            korenie bez soli 18g 1 ks  0,95 F
            Par. strapec 250g 1 ks     3,39 E
            NA ÚHRADU EUR        5,92
            KARTA                5,92
        """.trimIndent()
        assertEquals("5.92", ExtractReceiptAmountUseCase.parseTotal(text))
    }

    @Test
    fun `parses receipt with TOTAL label`() {
        val text = """
            Item 1         2.50
            Item 2         3.75
            TOTAL          6.25
        """.trimIndent()
        assertEquals("6.25", ExtractReceiptAmountUseCase.parseTotal(text))
    }

    @Test
    fun `parses comma-separated amount`() {
        val text = """
            Položka        12,50
            NA ÚHRADU EUR  12,50
        """.trimIndent()
        assertEquals("12.50", ExtractReceiptAmountUseCase.parseTotal(text))
    }

    @Test
    fun `falls back to largest amount when no label found`() {
        val text = """
            Something      3,50
            Another        7,25
            More           1,20
        """.trimIndent()
        assertEquals("7.25", ExtractReceiptAmountUseCase.parseTotal(text))
    }

    @Test
    fun `returns null for text without amounts`() {
        val text = "No numbers here at all"
        assertNull(ExtractReceiptAmountUseCase.parseTotal(text))
    }

    @Test
    fun `parses UHRADU without accent`() {
        val text = """
            Item           4,00
            NA UHRADU EUR  4,00
        """.trimIndent()
        assertEquals("4", ExtractReceiptAmountUseCase.parseTotal(text))
    }
}

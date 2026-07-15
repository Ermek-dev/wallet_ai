package kz.abdiyev.walletai

import kz.abdiyev.walletai.util.DescriptionNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals

class DescriptionNormalizerTest {

    @Test
    fun normalize_lowercasesAndCollapsesWhitespace() {
        assertEquals("magnum almaty", DescriptionNormalizer.normalize("  Magnum   Almaty  "))
    }

    @Test
    fun normalize_stripsPunctuation() {
        assertEquals("сұлтан с", DescriptionNormalizer.normalize("Сұлтан С."))
    }

    @Test
    fun normalize_treatsCaseAndPunctuationAsSameKey() {
        val first = DescriptionNormalizer.normalize("Покупка: Magnum!")
        val second = DescriptionNormalizer.normalize("покупка magnum")
        assertEquals(first, second)
    }
}

package com.aoto.kakeibo

import com.aoto.kakeibo.parse.AmountParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    @Test fun parsesCommaAmount() {
        assertEquals(1844L, AmountParser.extractYen("支払い:1,844円"))
    }

    @Test fun parsesPlainAmount() {
        assertEquals(680L, AmountParser.extractYen("ご利用 680円"))
    }

    @Test fun parsesFullWidthDigits() {
        assertEquals(1234L, AmountParser.extractYen("１，２３４円"))
    }

    @Test fun noAmountReturnsNull() {
        assertNull(AmountParser.extractYen("残高照会のお知らせ"))
        assertNull(AmountParser.extractYen(null))
        assertNull(AmountParser.extractYen(""))
    }

    @Test fun firstAmountWins() {
        assertEquals(1844L, AmountParser.extractYen("支払い:1,844円 残高 5,000円"))
    }
}

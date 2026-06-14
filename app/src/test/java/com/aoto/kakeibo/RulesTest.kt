package com.aoto.kakeibo

import com.aoto.kakeibo.parse.Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RulesTest {

    @Test fun idPaymentByPackage() {
        val p = Rules.parse(Rules.PKG_FELICA, "iD", "支払い:1,844円")
        assertEquals("iD", p?.source)
        assertEquals(1844L, p?.amount)
    }

    @Test fun idPaymentByTitle() {
        val p = Rules.parse("com.example.unknown", "iD", "支払い:500円")
        assertEquals("iD", p?.source)
        assertEquals(500L, p?.amount)
    }

    @Test fun vpassUsageWithMerchant() {
        val p = Rules.parse(
            "jp.co.smbc_card.vpass",
            "Vpass",
            "ご利用のお知らせ ご利用先：スーパー 2,500円"
        )
        assertEquals("Vpass", p?.source)
        assertEquals(2500L, p?.amount)
        assertEquals("スーパー", p?.merchant)
    }

    @Test fun ignoresUnrelatedApp() {
        assertNull(Rules.parse("com.example.sns", "新着", "いいねされました"))
    }

    @Test fun ignoresNonPaymentIdNotification() {
        // 「支払」を含まない iD 系通知は記録対象にしない
        assertNull(Rules.parse(Rules.PKG_FELICA, "iD", "残高を確認しました"))
    }

    @Test fun tracksKnownPackages() {
        assert(Rules.isTrackedPackage(Rules.PKG_FELICA))
        assert(Rules.isTrackedPackage("jp.co.smbc_card.vpass"))
        assert(!Rules.isTrackedPackage("com.example.sns"))
    }
}

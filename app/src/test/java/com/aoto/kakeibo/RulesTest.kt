package com.aoto.kakeibo

import com.aoto.kakeibo.parse.Rules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RulesTest {

    @Test fun idAppNotificationIsNotRecorded() {
        // iD（おサイフケータイ）の通知は二重計上回避のため記録しない。
        assertNull(Rules.parse(Rules.PKG_FELICA, "iD", "支払い:1,844円"))
    }

    @Test fun idTitleAloneIsNotRecorded() {
        // タイトルに「iD」があっても Vpass でなければ記録しない。
        assertNull(Rules.parse("com.example.unknown", "iD", "支払い:500円"))
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

    @Test fun vpassIdPaymentExtractsMerchant() {
        // Vpass 経由の iD 決済。「iD/利用先/iD」から利用先名を取り出す。
        val p = Rules.parse(
            "jp.co.smbc_card.vpass",
            "ご利用のお知らせ",
            "iD/業務スーパー　小田原東町店/iD 1,234円"
        )
        assertEquals("Vpass", p?.source)
        assertEquals(1234L, p?.amount)
        assertEquals("業務スーパー　小田原東町店", p?.merchant)
    }

    @Test fun vpassIdPaymentMyb() {
        val p = Rules.parse(
            "jp.co.smbc_card.vpass",
            "ご利用のお知らせ",
            "iD/MYB　小田原東町店/iD 980円"
        )
        assertEquals("Vpass", p?.source)
        assertEquals(980L, p?.amount)
        assertEquals("MYB　小田原東町店", p?.merchant)
    }

    @Test fun ignoresUnrelatedApp() {
        assertNull(Rules.parse("com.example.sns", "新着", "いいねされました"))
    }

    @Test fun tracksKnownPackages() {
        // iD（おサイフケータイ）は記録対象外。Vpass / SMBC のみ対象。
        assert(!Rules.isTrackedPackage(Rules.PKG_FELICA))
        assert(Rules.isTrackedPackage("jp.co.smbc_card.vpass"))
        assert(!Rules.isTrackedPackage("com.example.sns"))
    }
}

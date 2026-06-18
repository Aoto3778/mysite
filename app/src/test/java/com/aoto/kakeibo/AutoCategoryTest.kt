package com.aoto.kakeibo

import com.aoto.kakeibo.category.AutoCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoCategoryTest {

    @Test fun convenienceStoreIsFood() {
        assertEquals("食費", AutoCategory.guess("セブンイレブン", "Vpass"))
    }

    @Test fun drugstoreIsDaily() {
        assertEquals("日用品", AutoCategory.guess("マツモトキヨシ", "Vpass"))
    }

    @Test fun gyomuSuperBranchIsFood() {
        assertEquals("食費", AutoCategory.guess("業務スーパー　小田原東町店", "Vpass"))
    }

    @Test fun mybBranchIsFood() {
        assertEquals("食費", AutoCategory.guess("MYB　小田原東町店", "Vpass"))
    }

    @Test fun nullMerchantIsUnclassified() {
        assertEquals(AutoCategory.UNCLASSIFIED, AutoCategory.guess(null, "iD"))
    }

    @Test fun unknownMerchantIsUnclassified() {
        assertEquals(AutoCategory.UNCLASSIFIED, AutoCategory.guess("名もなき個人商店", "Vpass"))
    }

    // --- 履歴から追加したシード ---
    @Test fun seededMerchants() {
        assertEquals("食費", AutoCategory.guess("ザ・ビッグ小田原寿町", "Vpass"))
        assertEquals("食費", AutoCategory.guess("イトーヨーカドー　小田原店", "Vpass"))
        assertEquals("食費", AutoCategory.guess("ヨークマート　食品", "Vpass"))
        assertEquals("日用品", AutoCategory.guess("ドン・キホーテ小田原店", "Vpass"))
        assertEquals("通信", AutoCategory.guess("HISモバイル", "Vpass"))
        assertEquals("趣味・娯楽", AutoCategory.guess("PAYPAL *CRUNCHYROLL", "Vpass"))
        assertEquals("衣服・美容", AutoCategory.guess("HYPER Smart Salon", "Vpass"))
    }

    // --- ユーザー定義ルール ---
    @Test fun userRuleClassifiesUnknown() {
        val rules = listOf("個人商店" to "その他")
        assertEquals("その他", AutoCategory.guess("名もなき個人商店", "Vpass", rules))
    }

    @Test fun userRuleOverridesBuiltin() {
        val rules = listOf("セブン" to "その他")
        assertEquals("その他", AutoCategory.guess("セブンイレブン", "Vpass", rules))
    }

    @Test fun longerUserKeywordWins() {
        val rules = listOf("マート" to "食費", "ABCマート" to "衣服・美容")
        assertEquals("衣服・美容", AutoCategory.guess("ABCマート", "Vpass", rules))
    }
}

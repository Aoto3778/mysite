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
}

package com.aoto.kakeibo.parse

/** 通知本文から金額（円）を取り出す純粋ロジック。Android 非依存でテストできる。 */
object AmountParser {

    // 「1,844円」「1844円」などにマッチ。
    private val AMOUNT = Regex("""(\d{1,3}(?:,\d{3})+|\d+)\s*円""")

    /** 全角数字・記号を半角へ正規化する。 */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(
                when (ch) {
                    in '０'..'９' -> '0' + (ch - '０')
                    '，', '､', '、' -> ','
                    '　' -> ' '
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    fun extractYen(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val m = AMOUNT.find(normalize(text)) ?: return null
        return m.groupValues[1].replace(",", "").toLongOrNull()
    }
}

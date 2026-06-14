package com.aoto.kakeibo.parse

/** 解析結果。 */
data class ParsedTxn(val amount: Long, val merchant: String?, val source: String)

/**
 * 通知（パッケージ名・タイトル・本文）を見て、決済として記録すべきか判定する。
 * 対象は iD と Vpass。通知の文面が変わったときは、ここの条件と正規表現を調整する。
 */
object Rules {
    /** iD の決済通知の出どころ（おサイフケータイ）。 */
    const val PKG_FELICA = "com.felicanetworks.mfm.main"

    fun isTrackedPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return pkg == PKG_FELICA || p.contains("vpass") || p.contains("smbc")
    }

    fun parse(pkg: String, title: String?, text: String?): ParsedTxn? {
        val t = title.orEmpty()
        val body = text.orEmpty()
        val whole = "$t $body"

        // --- iD（おサイフケータイ）: 「iD」かつ「支払」を含み、金額が取れる ---
        val looksId = pkg == PKG_FELICA || t.contains("iD")
        if (looksId && whole.contains("支払")) {
            val amount = AmountParser.extractYen(whole) ?: return null
            return ParsedTxn(amount, merchant = null, source = "iD")
        }

        // --- Vpass（三井住友カード）: ご利用通知。利用先＋金額 ---
        val looksVpass = pkg.lowercase().let { it.contains("vpass") || it.contains("smbc") } ||
            whole.contains("Vpass") || whole.contains("三井住友")
        if (looksVpass && whole.contains("利用")) {
            val amount = AmountParser.extractYen(whole) ?: return null
            return ParsedTxn(amount, merchant = extractMerchant(body), source = "Vpass")
        }

        return null
    }

    /** 本文から利用先名らしき文字列を控えめに推定する（不明なら null）。 */
    private fun extractMerchant(body: String): String? {
        val labeled = Regex("""(?:ご利用先|利用先|利用店舗|加盟店)[：:\s]+([^\s／/\n]+)""").find(body)
        return labeled?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }
}

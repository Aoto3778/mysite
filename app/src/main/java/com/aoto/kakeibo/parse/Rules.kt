package com.aoto.kakeibo.parse

/** 解析結果。 */
data class ParsedTxn(val amount: Long, val merchant: String?, val source: String)

/**
 * 通知（パッケージ名・タイトル・本文）を見て、決済として記録すべきか判定する。
 *
 * 記録対象は Vpass（三井住友カード）の「ご利用通知」のみ。
 * iD（おサイフケータイ）の支払いは、同じ取引が後から Vpass 通知としても届くため、
 * iD アプリ側の通知も記録すると二重計上になる。よって iD 通知は記録しない。
 * Vpass 通知が iD 決済を表すときは「iD/利用先/iD」形式から利用先名を取り出す。
 */
object Rules {
    /** iD（おサイフケータイ）の通知パッケージ。二重計上回避のため記録対象外。 */
    const val PKG_FELICA = "com.felicanetworks.mfm.main"

    /** 「iD/利用先/iD」形式（全角・半角スラッシュ両対応）。利用先を group(1) で取る。 */
    private val ID_MERCHANT = Regex("""iD[／/]\s*(.+?)\s*[／/]iD""")

    /** 記録対象（Vpass）の通知かどうか。iD 単体アプリの通知は対象外。 */
    fun isTrackedPackage(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("vpass") || p.contains("smbc")
    }

    fun parse(pkg: String, title: String?, text: String?): ParsedTxn? {
        val t = title.orEmpty()
        val body = text.orEmpty()
        val whole = "$t $body"

        // --- Vpass（三井住友カード）のご利用通知のみ記録する ---
        // iD の支払いも後から Vpass 通知として届くため、iD/Vpass を問わずここで一括して扱う。
        val looksVpass = pkg.lowercase().let { it.contains("vpass") || it.contains("smbc") } ||
            whole.contains("Vpass") || whole.contains("三井住友")
        val looksIdPayment = ID_MERCHANT.containsMatchIn(whole)
        if (looksVpass && (whole.contains("利用") || looksIdPayment)) {
            val amount = AmountParser.extractYen(whole) ?: return null
            return ParsedTxn(amount, merchant = extractMerchant(whole), source = "Vpass")
        }

        return null
    }

    /**
     * 通知文から利用先名らしき文字列を推定する（不明なら null）。
     * iD 決済では「iD/業務スーパー　小田原東町店/iD」のように前後を「iD/」「/iD」で
     * 挟んだ形式で届くため、その中身を最優先で取り出す。
     */
    private fun extractMerchant(whole: String): String? {
        // 1) iD 決済の「iD/利用先/iD」形式を最優先で抽出する。
        ID_MERCHANT.find(whole)?.let {
            return it.groupValues[1].trim().ifBlank { null }
        }
        // 2) ラベル付き（ご利用先：◯◯）。値は空白手前まで控えめに拾う。
        val labeled = Regex("""(?:ご利用先|利用先|利用店舗|加盟店)[：:\s]+([^\s／/\n]+)""").find(whole)
        return labeled?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }
}

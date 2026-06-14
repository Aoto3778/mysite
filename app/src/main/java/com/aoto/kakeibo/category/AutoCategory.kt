package com.aoto.kakeibo.category

/** 利用先名からカテゴリを推定する。該当なしは「未分類」。 */
object AutoCategory {
    const val UNCLASSIFIED = "未分類"

    val CATEGORIES = listOf(
        "食費", "日用品", "交通", "交際費", "趣味・娯楽",
        "衣服・美容", "水道・光熱", "通信", "医療", "教養・教育",
        "その他", UNCLASSIFIED
    )

    private val MAP: List<Pair<String, List<String>>> = listOf(
        "食費" to listOf("コンビニ", "セブン", "ローソン", "ファミリーマート", "ファミマ", "ミニストップ", "スーパー", "マクドナルド", "すき家", "松屋", "吉野家", "スタバ", "スターバックス", "ドトール", "カフェ", "食堂", "レストラン", "イオン", "マルエツ", "ライフ", "業務スーパー"),
        "日用品" to listOf("ドラッグ", "マツモトキヨシ", "マツキヨ", "ウエルシア", "サンドラッグ", "ココカラ", "ダイソー", "セリア", "薬局", "ニトリ", "無印", "カインズ", "コーナン", "ホームセンター"),
        "交通" to listOf("JR", "メトロ", "東急", "京王", "小田急", "西武", "バス", "タクシー", "ENEOS", "出光", "コスモ", "ガソリン", "Suica", "PASMO", "ICOCA", "鉄道", "高速", "ETC", "駐車"),
        "通信" to listOf("ドコモ", "docomo", "ソフトバンク", "SoftBank", "au", "楽天モバイル", "携帯", "プロバイダ", "Wi-Fi"),
        "水道・光熱" to listOf("電気", "ガス", "水道", "東京電力", "関西電力", "中部電力", "東京ガス", "光熱"),
        "趣味・娯楽" to listOf("Amazon", "アマゾン", "書店", "ゲーム", "映画", "TOHO", "Steam", "Google Play", "Nintendo", "PlayStation", "Netflix", "Spotify", "カラオケ"),
        "衣服・美容" to listOf("ユニクロ", "UNIQLO", "GU", "美容", "理容", "ZOZO", "しまむら", "コスメ", "H&M"),
        "医療" to listOf("病院", "クリニック", "歯科", "調剤", "薬")
    )

    fun guess(merchant: String?, source: String): String {
        val m = merchant ?: return UNCLASSIFIED
        for ((cat, keys) in MAP) {
            if (keys.any { m.contains(it, ignoreCase = true) }) return cat
        }
        return UNCLASSIFIED
    }
}

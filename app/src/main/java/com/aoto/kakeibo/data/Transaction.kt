package com.aoto.kakeibo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 1件の取引（支出または収入）。支出は通知から自動生成されるか手動入力、収入は手動入力のみ。 */
@Entity(tableName = "transactions")
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,                 // 発生日時（epoch millis）
    val amount: Long,                    // 金額（円・正の値）
    val source: String,                  // "iD" / "Vpass" / "現金" / "収入"
    val merchant: String? = null,        // 利用先 / 収入の内容
    val category: String = "未分類",
    val rawText: String? = null,         // 元の通知本文（自動記録のとき）
    val note: String? = null,
    val isManual: Boolean = false,
    val isIncome: Boolean = false,       // true=収入 / false=支出
    val deleted: Boolean = false
)

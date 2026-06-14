package com.aoto.kakeibo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 1件の支出。決済通知から自動生成されるか、手動入力される。 */
@Entity(tableName = "transactions")
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,                 // 発生日時（epoch millis）
    val amount: Long,                    // 金額（円）
    val source: String,                  // "iD" / "Vpass" / "現金" / "手動"
    val merchant: String? = null,        // 利用先
    val category: String = "未分類",
    val rawText: String? = null,         // 元の通知本文（自動記録のとき）
    val note: String? = null,
    val isManual: Boolean = false,
    val deleted: Boolean = false
)

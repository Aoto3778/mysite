package com.aoto.kakeibo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ユーザーが手動で設定する「利用先のワード → カテゴリ」の分類ルール。
 * 例: keyword="Salon", category="衣服・美容"。
 * 以後、利用先名にこの語を含む決済は自動的にこのカテゴリへ分類される。
 */
@Entity(tableName = "category_rules")
data class RuleEntity(
    @PrimaryKey val keyword: String,   // 利用先に含まれる語（大文字小文字は無視して照合）
    val category: String,              // 割り当てるカテゴリ
    val createdAt: Long = System.currentTimeMillis()
)

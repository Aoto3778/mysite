package com.aoto.kakeibo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 解析できなかった対象アプリの通知（または診断モード時の全通知）を生のまま記録する。
 * Vpass など未知の文面を後から確認し、解析ルールを調整するために使う。
 */
@Entity(tableName = "raw_captures")
data class RawCapture(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val pkg: String,
    val title: String? = null,
    val text: String? = null,
    val handled: Boolean = false
)

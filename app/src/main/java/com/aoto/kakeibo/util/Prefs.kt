package com.aoto.kakeibo.util

import android.content.Context

/** 設定の保存（SharedPreferences）。 */
object Prefs {
    private const val FILE = "kakeibo_prefs"
    private const val KEY_CAPTURE_ALL = "capture_all"

    private fun sp(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** 診断モード: すべてのアプリの通知を生データとして記録する（Vpass等の文面確認用）。 */
    fun captureAll(context: Context): Boolean =
        sp(context).getBoolean(KEY_CAPTURE_ALL, false)

    fun setCaptureAll(context: Context, value: Boolean) {
        sp(context).edit().putBoolean(KEY_CAPTURE_ALL, value).apply()
    }
}

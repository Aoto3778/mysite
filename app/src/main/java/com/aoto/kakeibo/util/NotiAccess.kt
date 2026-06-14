package com.aoto.kakeibo.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.aoto.kakeibo.noti.NotiListenerService

/** 通知アクセス権限の状態確認と、各種設定画面を開くヘルパ。 */
object NotiAccess {

    fun isGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val target = ComponentName(context, NotiListenerService::class.java)
        return flat.split(":").any { entry ->
            val cn = ComponentName.unflattenFromString(entry)
            cn != null && cn.packageName == context.packageName && cn == target
        }
    }

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openBatterySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

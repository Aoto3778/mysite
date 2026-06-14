package com.aoto.kakeibo.noti

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aoto.kakeibo.category.AutoCategory
import com.aoto.kakeibo.data.AppDatabase
import com.aoto.kakeibo.data.RawCapture
import com.aoto.kakeibo.data.Repository
import com.aoto.kakeibo.data.TxnEntity
import com.aoto.kakeibo.parse.Rules
import com.aoto.kakeibo.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 決済通知を読み取る本体。ユーザーが「通知へのアクセス」を許可するとシステムがバインドし、
 * 通知が届くたびに onNotificationPosted が呼ばれる。
 */
class NotiListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repo: Repository by lazy {
        val db = AppDatabase.get(applicationContext)
        Repository(db.txnDao(), db.rawDao())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val pkg = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        val captureAll = Prefs.captureAll(applicationContext)

        scope.launch {
            val parsed = Rules.parse(pkg, title, text)
            if (parsed != null) {
                // 同一通知の二重計上を防ぐ（±90秒・同一 source / 金額）。
                val dup = repo.countSimilar(
                    parsed.source, parsed.amount, postTime - 90_000, postTime + 90_000
                )
                if (dup == 0) {
                    repo.insert(
                        TxnEntity(
                            timestamp = postTime,
                            amount = parsed.amount,
                            source = parsed.source,
                            merchant = parsed.merchant,
                            category = AutoCategory.guess(parsed.merchant, parsed.source),
                            rawText = listOfNotNull(title, text).joinToString(" / "),
                            isManual = false
                        )
                    )
                }
            } else if (captureAll || Rules.isTrackedPackage(pkg)) {
                // 解析できなかった対象アプリの通知は生データとして残し、後で文面を確認・調整する。
                repo.insertRaw(
                    RawCapture(timestamp = postTime, pkg = pkg, title = title, text = text, handled = false)
                )
            }
        }
    }
}

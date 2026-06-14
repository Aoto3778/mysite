package com.aoto.kakeibo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aoto.kakeibo.util.NotiAccess
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun yen(amount: Long): String =
    if (amount < 0) "-¥%,d".format(-amount) else "¥%,d".format(amount)

fun monthLabel(ym: YearMonth): String = "${ym.year}年${ym.monthValue}月"

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPANESE)
private val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.JAPANESE)

fun localDateOf(ts: Long): LocalDate =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

fun dateLabel(ts: Long): String = localDateOf(ts).format(DATE_FMT)

fun timeLabel(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalTime().format(TIME_FMT)

/** 通知アクセスの許可状態を返し、画面復帰（ON_RESUME）のたびに再確認する。 */
@Composable
fun rememberNotiGranted(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(NotiAccess.isGranted(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = NotiAccess.isGranted(context)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return granted
}

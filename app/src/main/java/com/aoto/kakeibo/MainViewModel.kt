package com.aoto.kakeibo

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aoto.kakeibo.category.AutoCategory
import com.aoto.kakeibo.data.RawCapture
import com.aoto.kakeibo.data.TxnEntity
import com.aoto.kakeibo.util.Prefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext: Application = app
    private val repo = (app as KakeiboApp).repository

    val txns: StateFlow<List<TxnEntity>> =
        repo.observeTxns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rawCaptures: StateFlow<List<RawCapture>> =
        repo.observeRaw().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var selectedMonth by mutableStateOf(YearMonth.now())
        private set

    var captureAll by mutableStateOf(Prefs.captureAll(app))
        private set

    fun prevMonth() { selectedMonth = selectedMonth.minusMonths(1) }
    fun nextMonth() { selectedMonth = selectedMonth.plusMonths(1) }

    fun updateCaptureAll(value: Boolean) {
        captureAll = value
        Prefs.setCaptureAll(appContext, value)
    }

    fun monthOf(ts: Long): YearMonth {
        val d = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
        return YearMonth.of(d.year, d.monthValue)
    }

    fun addManual(
        amount: Long,
        merchant: String?,
        category: String,
        timestamp: Long,
        note: String?,
        source: String
    ) {
        viewModelScope.launch {
            repo.insert(
                TxnEntity(
                    timestamp = timestamp,
                    amount = amount,
                    source = source,
                    merchant = merchant?.ifBlank { null },
                    category = category.ifBlank { AutoCategory.UNCLASSIFIED },
                    note = note?.ifBlank { null },
                    isManual = true
                )
            )
        }
    }

    fun save(txn: TxnEntity) {
        viewModelScope.launch { repo.update(txn) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearRaw() {
        viewModelScope.launch { repo.clearRaw() }
    }

    /** 全件を CSV 文字列にする（バックアップ用）。 */
    suspend fun csvForExport(): String {
        val all = repo.getAll().sortedBy { it.timestamp }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        fun esc(s: String?) = "\"" + (s ?: "").replace("\"", "\"\"") + "\""
        val sb = StringBuilder("日時,種別,金額,カテゴリ,利用先,メモ\n")
        for (t in all) {
            val dt = Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault()).format(fmt)
            sb.append("$dt,${t.source},${t.amount},${esc(t.category)},${esc(t.merchant)},${esc(t.note)}\n")
        }
        return sb.toString()
    }
}

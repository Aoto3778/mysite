package com.aoto.kakeibo.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aoto.kakeibo.MainViewModel
import com.aoto.kakeibo.util.NotiAccess
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val granted = rememberNotiGranted()
    val raw by vm.rawCaptures.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val csv = vm.csvForExport()
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }.isSuccess
                Toast.makeText(
                    context,
                    if (ok) "CSVを書き出しました" else "書き出しに失敗しました",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        SectionTitle("通知アクセス")
        Text(
            if (granted) "状態：許可済み" else "状態：未許可",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { NotiAccess.openSettings(context) }) { Text("通知アクセス設定を開く") }
        Spacer(Modifier.height(24.dp))

        SectionTitle("取りこぼし防止")
        Text(
            "OSにアプリを止められないよう、バッテリー最適化から本アプリ・おサイフケータイ・Vpassを除外してください。",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { NotiAccess.openBatterySettings(context) }) {
            Text("バッテリー最適化の設定を開く")
        }
        Spacer(Modifier.height(24.dp))

        SectionTitle("バックアップ")
        Text("全データをCSVファイルに書き出します。", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = {
            exportLauncher.launch("kakeibo-${System.currentTimeMillis()}.csv")
        }) { Text("CSVに書き出す") }
        Spacer(Modifier.height(24.dp))

        SectionTitle("診断（Vpassなどの文面確認）")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("すべての通知をキャプチャ", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "未対応の決済通知の文面を確認するための一時モード。確認後はOFFにしてください。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = vm.captureAll, onCheckedChange = { vm.updateCaptureAll(it) })
        }
        Spacer(Modifier.height(8.dp))
        if (raw.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最近キャプチャした通知 ${raw.size} 件", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { vm.clearRaw() }) { Text("クリア") }
            }
            raw.take(20).forEach { r ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(r.pkg, style = MaterialTheme.typography.labelSmall)
                        Text(
                            r.title ?: "(タイトルなし)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(r.text ?: "(本文なし)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            Text("まだキャプチャはありません。", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(28.dp))

        SectionTitle("このアプリについて")
        Text(
            "iD / Vpass の決済通知を自動で記録する個人用の家計簿です。データは端末内のみに保存され、外部へ送信されません（通信機能なし）。",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(4.dp))
        Text("バージョン 1.0", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
}

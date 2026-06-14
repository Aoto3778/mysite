package com.aoto.kakeibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aoto.kakeibo.data.TxnEntity
import com.aoto.kakeibo.ui.EditorDialog
import com.aoto.kakeibo.ui.HomeScreen
import com.aoto.kakeibo.ui.KakeiboTheme
import com.aoto.kakeibo.ui.SettingsScreen
import com.aoto.kakeibo.ui.SummaryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KakeiboTheme { AppRoot() } }
    }
}

private enum class Tab { Home, Summary, Settings }

@Composable
private fun AppRoot(vm: MainViewModel = viewModel()) {
    val txns by vm.txns.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.Home) }
    var editor by remember { mutableStateOf<TxnEntity?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Home,
                    onClick = { tab = Tab.Home },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("ホーム") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Summary,
                    onClick = { tab = Tab.Summary },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                    label = { Text("集計") }
                )
                NavigationBarItem(
                    selected = tab == Tab.Settings,
                    onClick = { tab = Tab.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("設定") }
                )
            }
        },
        floatingActionButton = {
            if (tab == Tab.Home) {
                FloatingActionButton(onClick = {
                    editor = TxnEntity(
                        timestamp = System.currentTimeMillis(),
                        amount = 0,
                        source = "現金",
                        isManual = true
                    )
                }) { Icon(Icons.Filled.Add, contentDescription = "手動で追加") }
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (tab) {
            Tab.Home -> HomeScreen(vm, txns, mod) { editor = it }
            Tab.Summary -> SummaryScreen(vm, txns, mod)
            Tab.Settings -> SettingsScreen(vm, mod)
        }
    }

    editor?.let { current ->
        EditorDialog(
            initial = current,
            onDismiss = { editor = null },
            onSave = { saved ->
                vm.upsert(saved)
                editor = null
            },
            onDelete = if (current.id != 0L) ({ vm.delete(current.id); editor = null }) else null
        )
    }
}

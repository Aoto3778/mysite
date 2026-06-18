package com.aoto.kakeibo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aoto.kakeibo.MainViewModel
import com.aoto.kakeibo.category.AutoCategory

/**
 * 「利用先に含まれるワード → カテゴリ」のユーザー定義ルールを管理する。
 * 追加すると以後その語を含む決済が自動分類され、既存の未分類取引にも反映される。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRulesSection(vm: MainViewModel, modifier: Modifier = Modifier) {
    val rules by vm.rules.collectAsStateWithLifecycle()
    var keyword by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AutoCategory.CATEGORIES.first()) }
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Text(
            "利用先に含まれるワードを好きなカテゴリに割り当てます。例：「Salon」→ 衣服・美容。" +
                "以後その語を含む決済は自動でそのカテゴリになり、未分類の既存データにも反映します。",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("ワード（例：Salon）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        ExposedDropdownMenuBox(expanded = menuOpen, onExpandedChange = { menuOpen = it }) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("カテゴリ") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                AutoCategory.CATEGORIES.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = { category = c; menuOpen = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { vm.addRule(keyword, category); keyword = "" },
            enabled = keyword.isNotBlank()
        ) { Text("ルールを追加") }
        Spacer(Modifier.height(12.dp))

        if (rules.isEmpty()) {
            Text("まだルールはありません。", style = MaterialTheme.typography.bodySmall)
        } else {
            rules.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "「${r.keyword}」→ ${r.category}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    TextButton(onClick = { vm.deleteRule(r.keyword) }) { Text("削除") }
                }
                HorizontalDivider()
            }
        }
    }
}

package com.example.browser_queries_db.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ShowTableDialog(
    tables: List<String>,
    onDismiss: () -> Unit,
    onShowTable: (String) -> Unit
) {
    var selectedTable by remember { mutableStateOf(tables.firstOrNull().orEmpty()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Показ таблицы") },
        text = {
            Column {
                Text("Таблица:")

                Box {
                    OutlinedButton(
                        onClick = {
                            dropdownExpanded = true
                        }
                    ) {
                        Text(
                            if (selectedTable.isBlank()) {
                                "Выберите таблицу"
                            } else {
                                selectedTable
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = {
                            dropdownExpanded = false
                        }
                    ) {
                        for (table in tables) {
                            DropdownMenuItem(
                                text = {
                                    Text(table)
                                },
                                onClick = {
                                    selectedTable = table
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (tables.isEmpty()) {
                    Text("Нет доступных таблиц")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedTable.isNotBlank(),
                onClick = {
                    onShowTable(selectedTable)
                }
            ) {
                Text("Показать")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

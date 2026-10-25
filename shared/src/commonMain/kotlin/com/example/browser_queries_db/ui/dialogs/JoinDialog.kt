package com.example.browser_queries_db.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun JoinDialog(
    firstTable: String,
    secondTable: String,
    firstColumns: List<String>,
    secondColumns: List<String>,
    onDismiss: () -> Unit,
    onApply: (firstColumn: String, secondColumn: String) -> Unit
) {
    var firstColumn by remember(firstColumns) {
        mutableStateOf(firstColumns.firstOrNull().orEmpty())
    }

    var secondColumn by remember(secondColumns) {
        mutableStateOf(secondColumns.firstOrNull().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Соединение таблиц")
        },

        text = {
            Column {
                Text("$firstTable = $secondTable")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Условие соединения")

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    JoinColumnDropdown(
                        columns = firstColumns,
                        selectedColumn = firstColumn,
                        onSelect = { firstColumn = it }
                    )

                    Text(
                        text = "=",
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )

                    JoinColumnDropdown(
                        columns = secondColumns,
                        selectedColumn = secondColumn,
                        onSelect = { secondColumn = it }
                    )
                }
            }
        },

        confirmButton = {
            Button(
                enabled = firstColumn.isNotBlank() && secondColumn.isNotBlank(),
                onClick = {
                    onApply(firstColumn, secondColumn)
                }
            ) {
                Text("Соединить")
            }
        },

        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun JoinColumnDropdown(
    columns: List<String>,
    selectedColumn: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = {
                expanded = true
            }
        ) {
            Text(
                selectedColumn.ifBlank {
                    "Выберите колонку"
                }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            for (column in columns) {
                DropdownMenuItem(
                    text = {
                        Text(column)
                    },
                    onClick = {
                        onSelect(column)
                        expanded = false
                    }
                )
            }
        }
    }
}

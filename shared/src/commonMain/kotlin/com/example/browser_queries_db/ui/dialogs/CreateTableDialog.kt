package com.example.browser_queries_db.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser_queries_db.model.ColumnDefinition
import com.example.browser_queries_db.ui.components.ColumnDraft
import com.example.browser_queries_db.ui.components.ColumnRow

@Composable
fun CreateTableDialog(
    onApply: (tableName: String, columnDefinitions: List<ColumnDefinition>) -> Unit,
    onDismiss: () -> Unit,
){
    var tableName by remember { mutableStateOf("") }
    var columnCount by remember { mutableStateOf("") }
    val count = columnCount.toIntOrNull() ?: 0

    val columns = remember { mutableStateListOf<ColumnDraft>() }

    // Синхронизируем количество черновиков колонок с введенным числом
    LaunchedEffect(count) {
        if (columns.size < count) {
            val itemsToAdd = count - columns.size
            repeat(itemsToAdd) {
                columns.add(ColumnDraft())
            }
        } else if (columns.size > count) {
            while (columns.size > count) {
                columns.removeLast()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создание таблицы") },
        text = {
            Column {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Введите название таблицы:")
                    TextField(
                        value = tableName,
                        onValueChange = { tableName = it },
                    )

                    Text("Количество столбцов:")
                    TextField(
                        value = columnCount,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() }) {
                                columnCount = newValue
                            }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Колонки:
                // Добавлена проверка columns.size == count, чтобы дождаться окончания работы LaunchedEffect
                if (count > 0 && columns.size == count) {
                    columns.forEachIndexed { i, column ->
                        ColumnRow(
                            number = i + 1,
                            columnName = column.name,
                            columnType = column.type,
                            isNullable = column.nullable,
                            onColumnNameChange = { newName ->
                                columns[i] = columns[i].copy(name = newName)
                            },
                            onColumnTypeChange = { newType ->
                                columns[i] = columns[i].copy(type = newType)
                            },
                            onNullableChange = { newNullable ->
                                columns[i] = columns[i].copy(nullable = newNullable)
                            }
                        )

                        if (i < columns.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                // Опционально: кнопка заблокирована, если не введено имя таблицы или число колонок <= 0
                enabled = tableName.isNotBlank() && count > 0,
                onClick = {
                    val userInput = columns.map { column ->
                        ColumnDefinition(column.name, column.type, column.nullable)
                    }
                    onApply(tableName, userInput)
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

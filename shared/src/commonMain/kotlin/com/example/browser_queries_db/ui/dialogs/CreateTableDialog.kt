package com.example.browser_queries_db.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment

@Composable
fun CreateTableDialog(
    onApply: (tableName: String, columnDefinitions: List<ColumnDefinition>) -> Unit,
    onDismiss: () -> Unit,
){
    val datatypes = setOf(
        "string",
        "int",
        "data"
    )

    var tableName by remember { mutableStateOf("") }
    var columnCount by remember { mutableStateOf("") }
    val count = columnCount.toIntOrNull() ?: 0

    val columnNames = remember { mutableStateListOf<String>() }
    val dropdownExpanded = remember { mutableStateListOf<Boolean>() } // Исправлено: теперь синхронизируется
    val selectedDatatypes = remember { mutableStateListOf<String>() }
    val columnNullability = remember { mutableStateListOf<Boolean>() }

    // Синхронизируем размеры ВСЕХ списков с введенным числом
    LaunchedEffect(count) {
        if (columnNames.size < count) {
            val itemsToAdd = count - columnNames.size
            repeat(itemsToAdd) {
                columnNames.add("")
                selectedDatatypes.add(datatypes.first())
                columnNullability.add(false)
                dropdownExpanded.add(false)
            }
        } else if (columnNames.size > count) {
            while (columnNames.size > count) {
                columnNames.removeLast()
                selectedDatatypes.removeLast()
                columnNullability.removeLast()
                dropdownExpanded.removeLast()
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
                // Добавлена проверка columnNames.size == count, чтобы дождаться окончания работы LaunchedEffect
                if (count > 0 && columnNames.size == count) {
                    columnNames.forEachIndexed { i, currentName ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),

                        ) {
                            // Секция ввод названия
                            Column {
                                Text("Название колонки №${i + 1}:")
                                TextField(
                                    value = currentName,
                                    onValueChange = { newValue ->
                                        columnNames[i] = newValue
                                    }
                                )
                            }

                            // Секция - выбор типа данных
                            Box {
                                OutlinedButton(
                                    onClick = {
                                        dropdownExpanded[i] = true
                                    }
                                ) {
                                    Text(selectedDatatypes[i])
                                }

                                DropdownMenu(
                                    expanded = dropdownExpanded[i],
                                    onDismissRequest = { dropdownExpanded[i] = false }
                                ) {
                                    for (datatype in datatypes) {
                                        DropdownMenuItem(
                                            text = { Text(datatype) },
                                            onClick = {
                                                selectedDatatypes[i] = datatype
                                                dropdownExpanded[i] = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Секция - поддержка null
                            Text("nullable:")
                            Checkbox(
                                checked = columnNullability[i],
                                onCheckedChange = { newValue ->
                                    columnNullability[i] = newValue
                                }
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
                    val userInput = columnNames.mapIndexed { index, column ->
                        ColumnDefinition(column, selectedDatatypes[index], columnNullability[index])
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

package com.example.browser_queries_db.ui.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser_queries_db.getTableHeaders
import com.example.browser_queries_db.model.Actions
import com.example.browser_queries_db.model.UpdateColumnQuery
import com.example.browser_queries_db.ui.components.COLUMN_TYPES
import com.example.browser_queries_db.ui.components.ColumnRow
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Колонка в форме редактирования.
// originalName == null означает, что колонка добавлена пользователем.
private data class EditableColumn(
    val originalName: String? = null,
    val name: String = "",
    val type: String = COLUMN_TYPES.first(),
    val nullable: Boolean = false,
    val originalType: String? = null,
    val originalNullable: Boolean? = null
)

@Composable
fun EditTableDialog(
    tables: List<String>,
    onDismiss: () -> Unit,
    onApply: (tableName: String, columns: List<UpdateColumnQuery>) -> Unit
) {
    var selectedTable by remember { mutableStateOf(tables.firstOrNull().orEmpty()) }
    var tableDropdownExpanded by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Колонки, загруженные из БД (для определения удалённых колонок)
    val loadedColumns = remember { mutableStateListOf<EditableColumn>() }

    // Текущее состояние формы
    val columns = remember { mutableStateListOf<EditableColumn>() }

    // При выборе таблицы загружаем её колонки
    LaunchedEffect(selectedTable) {
        loadedColumns.clear()
        columns.clear()
        error = null

        if (selectedTable.isBlank()) {
            return@LaunchedEffect
        }

        loading = true

        try {
            val headers = getTableHeaders(selectedTable)

            for (header in headers) {
                val columnObject = header.jsonObject

                val name = columnObject["column_name"]
                    ?.jsonPrimitive
                    ?.content
                    .orEmpty()

                val sqlType = columnObject["data_type"]
                    ?.jsonPrimitive
                    ?.content
                    .orEmpty()

                val clientType = sqlTypeToColumnType(sqlType)

                val nullable = columnObject["is_nullable"]
                    ?.jsonPrimitive
                    ?.content == "YES"

                loadedColumns.add(
                    EditableColumn(
                        originalName = name,
                        name = name,
                        type = clientType,
                        nullable = nullable,
                        originalType = clientType,
                        originalNullable = nullable
                    )
                )
            }

            columns.addAll(loadedColumns)
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    // Формируем список изменений для PATCH-запроса
    val changes = buildList {
        // Новые и изменённые колонки
        columns.forEach { column ->
            if (column.originalName == null) {
                add(
                    UpdateColumnQuery(
                        name = column.name,
                        type = column.type,
                        isNullable = column.nullable,
                        action = Actions.ADD
                    )
                )
            } else if (
                column.type != column.originalType ||
                column.nullable != column.originalNullable
            ) {
                add(
                    UpdateColumnQuery(
                        name = column.originalName,
                        type = column.type,
                        isNullable = column.nullable,
                        action = Actions.ALTER
                    )
                )
            }
        }

        // Удалённые колонки
        loadedColumns
            // Находим те колонки, которые были загружены, но теперь не найдены в новом списке колонок
            .filter { loaded ->
                columns.none { column ->
                    column.originalName == loaded.originalName
                }
            }
            .forEach { column ->
                add(
                    UpdateColumnQuery(
                        name = column.originalName.orEmpty(),
                        type = column.type,
                        isNullable = column.nullable,
                        action = Actions.DELETE
                    )
                )
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование таблицы") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Таблица:")

                // Выпадающий список с таблицей
                Box {
                    OutlinedButton(
                        onClick = {
                            tableDropdownExpanded = true
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
                        expanded = tableDropdownExpanded,
                        onDismissRequest = {
                            tableDropdownExpanded = false
                        }
                    ) {
                        for (table in tables) {
                            DropdownMenuItem(
                                text = {
                                    Text(table)
                                },
                                onClick = {
                                    selectedTable = table
                                    tableDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                when {
                    loading -> {
                        Text("Загрузка...")
                    }

                    error != null -> {
                        Text("Ошибка: $error")
                    }

                    selectedTable.isBlank() -> {
                        Text("Нет доступных таблиц")
                    }

                    else -> {
                        columns.forEachIndexed { i, column ->
                            val isId = column.originalName
                                ?.equals("id", ignoreCase = true) == true

                            ColumnRow(
                                number = i + 1,
                                columnName = column.name,
                                columnType = column.type,
                                isNullable = column.nullable,
                                // Разрешаем переименовывать только новые колонки
                                nameEnabled = column.originalName == null,
                                onColumnNameChange = { newName ->
                                    columns[i] = columns[i].copy(name = newName)
                                },
                                onColumnTypeChange = { newType ->
                                    columns[i] = columns[i].copy(type = newType)
                                },
                                onNullableChange = { newNullable ->
                                    columns[i] = columns[i].copy(nullable = newNullable)
                                },
                                onDelete = if (isId) {
                                    null
                                } else {
                                    {
                                        columns.removeAt(i)
                                    }
                                }
                            )

                            if (i < columns.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                columns.add(EditableColumn())
                            }
                        ) {
                            Text("Добавить колонку")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedTable.isNotBlank() &&
                        !loading &&
                        error == null &&
                        changes.isNotEmpty() &&
                        columns.all { it.name.isNotBlank() },
                onClick = {
                    onApply(selectedTable, changes)
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

/*
 * Переводит SQL-тип колонки в тип, используемый формами приложения.
 */
private fun sqlTypeToColumnType(sqlType: String): String {
    return when (sqlType.lowercase()) {
        "integer", "smallint", "bigint" -> "int"
        "double precision", "real", "numeric" -> "double"
        "date" -> "date"
        else -> "string"
    }
}

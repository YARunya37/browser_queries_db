package com.example.browser_queries_db.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Контролирует редактирование таблицы и хранит состояние изменений.
 *
 * Сохраняет исходное состояние строк, отслеживает признак наличия изменений
 * и отображает кнопки «Отмена» и «Сохранить». Любое изменение ячейки,
 * удаление или добавление строки переводит признак isSaved в false.
 *
 * @param rows исходные строки таблицы.
 * @param columns имена колонок в порядке отображения.
 * @param onCancel вызывается при нажатии кнопки «Отмена».
 * @param onSave вызывается при сохранении с текущим состоянием строк в формате JSON.
 */
@Composable
fun TableEditor(
    rows: JsonArray,
    columns: List<String>,
    onCancel: () -> Unit,
    onSave: (JsonArray) -> Unit = {}
) {
    // Начальное состояние таблицы для последующего сравнения изменений
    val initialRows = remember(rows, columns) {
        // Преобразуем все строки таблицы если полученные извне строки или колонки изменились
        rows.toEditableRows(columns)
    }

    val editableRows = remember(rows, columns) {
        mutableStateListOf<EditableRow>().apply {
            addAll(initialRows)
        }
    }

    var isSaved by remember(rows, columns) {
        mutableStateOf(true)
    }

    fun updateCell(rowIndex: Int, column: String, value: String) {
        val row = editableRows.getOrNull(rowIndex) ?: return

        editableRows[rowIndex] = row.copy(
            cells = row.cells + (column to value)
        )
        isSaved = false
    }

    fun deleteRow(rowIndex: Int) {
        if (rowIndex !in editableRows.indices) {
            return
        }

        editableRows.removeAt(rowIndex)
        isSaved = false
    }

    fun addRow() {
        editableRows.add(
            EditableRow(
                cells = columns.associateWith { "" }
            )
        )
        isSaved = false
    }

    EditableDatabaseTable(
        rows = editableRows,
        columns = columns,
        onCellChange = ::updateCell,
        onDeleteRow = ::deleteRow,
        onAddRow = if (columns.isEmpty()) null else ::addRow
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onCancel
        ) {
            Text("Отмена")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            enabled = !isSaved,
            onClick = {
                onSave(editableRows.toJsonArray(columns))
                isSaved = true
            }
        ) {
            Text("Сохранить")
        }
    }
}

/**
 * Преобразует JSON-массив строк в редактируемые строки.
 *
 * Значения колонок приводятся к строкам; null и отсутствующие значения
 * становятся пустыми строками.
 *
 * @param columns имена колонок таблицы.
 * @return строки, готовые для редактирования.
 */
private fun JsonArray.toEditableRows(columns: List<String>): List<EditableRow> {
    return map { element ->
        val rowObject = element.jsonObject

        val cells = columns.associateWith { column ->
            val value = rowObject[column]

            when {
                value == null || value is JsonNull -> ""
                else -> value.jsonPrimitive.content
            }
        }

        EditableRow(cells = cells)
    }
}

/**
 * Преобразует редактируемые строки обратно в JSON-массив.
 *
 * @param columns имена колонок таблицы.
 * @return JSON-массив строк таблицы.
 */
private fun List<EditableRow>.toJsonArray(columns: List<String>): JsonArray {
    return JsonArray(
        map { row ->
            buildJsonObject {
                for (column in columns) {
                    put(column, JsonPrimitive(row.cells[column].orEmpty()))
                }
            }
        }
    )
}

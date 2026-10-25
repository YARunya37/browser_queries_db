package com.example.browser_queries_db.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser_queries_db.model.TableRowChanges
import com.example.browser_queries_db.model.UpdatedRow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Колонка идентификатора есть во всех таблицах и всегда называется id.
// У новых строк она пустая: id назначается БД после вставки строки.
private const val ID_COLUMN = "id"

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
 * @param onSave вызывается при сохранении с набором добавленных,
 *               изменённых и удалённых строк.
 */
@Composable
fun TableEditor(
    rows: JsonArray,
    columns: List<String>,
    onCancel: () -> Unit,
    onSave: suspend (TableRowChanges) -> Unit
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

    var isSaving by remember(rows, columns) {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    // Копируем изменения в editableRows
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
        // Новая строка ещё не сохранена, поэтому её id остаётся пустым
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
            enabled = !isSaving,
            onClick = onCancel
        ) {
            Text("Отмена")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            enabled = !isSaved && !isSaving,
            onClick = {
                val changes = editableRows.toRowChanges(
                    initialRows = initialRows,
                    columns = columns
                )

                scope.launch {
                    isSaving = true

                    try {
                        onSave(changes)
                    } finally {
                        isSaving = false
                    }
                }
            }
        ) {
            Text(if (isSaving) "Сохранение..." else "Сохранить")
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
 * Вычисляет изменения между исходными и текущими строками.
 *
 * Строки с пустым id считаются новыми и попадают в addedRows.
 * Для существующих строк id назначается БД и приходит при чтении таблицы.
 *
 * @param initialRows строки на момент открытия редактора.
 * @param columns имена колонок таблицы.
 * @return добавленные, изменённые и удалённые строки.
 */
private fun List<EditableRow>.toRowChanges(
    initialRows: List<EditableRow>,
    columns: List<String>
): TableRowChanges {
    val initialById = initialRows
        .mapNotNull { row ->
            row.rowId()?.let { id -> id to row }
        }
        .toMap()

    val currentIds = mapNotNull { it.rowId() }.toSet()

    // Удаленные строки. Разность множества изначальных id и текущих. Получаем тех, кого не стало
    val deletedRowIds = initialById.keys - currentIds

    // Находим строки у которых нет id(новые)
    val addedRows = filter { it.rowId() == null }
        .map { row -> row.toValues(columns) }

    // Фильтруем строки. Оставляем только те, которые изменились
    val updatedRows = mapNotNull { row ->
        val id = row.rowId() ?: return@mapNotNull null
        val initialRow = initialById[id] ?: return@mapNotNull null

        val changedValues = columns
            .filter { column -> column != ID_COLUMN }
            .filter { column ->
                row.cells[column] != initialRow.cells[column]
            }
            .associateWith { column ->
                row.cells[column].orEmpty()
            }

        if (changedValues.isEmpty()) {
            null
        } else {
            UpdatedRow(id = id, values = changedValues)
        }
    }

    return TableRowChanges(
        addedRows = addedRows,
        updatedRows = updatedRows,
        deletedRowIds = deletedRowIds.toList()
    )
}

/**
 * Возвращает идентификатор строки из колонки id.
 *
 * @return идентификатор или null для новой строки.
 */
private fun EditableRow.rowId(): Long? {
    return cells[ID_COLUMN]?.toLongOrNull()
}

/**
 * Возвращает значения колонок строки без колонки id.
 *
 * @param columns имена колонок таблицы.
 * @return значения колонок: имя -> значение.
 */
private fun EditableRow.toValues(
    columns: List<String>
): Map<String, String?> {
    return columns
        .filter { column -> column != ID_COLUMN }
        .associateWith { column ->
            cells[column].orEmpty()
        }
}

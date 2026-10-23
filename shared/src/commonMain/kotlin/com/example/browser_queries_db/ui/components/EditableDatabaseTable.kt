package com.example.browser_queries_db.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Строка таблицы в режиме редактирования.
 *
 * @property cells значения всех колонок, включая id: имя колонки -> текст.
 */
data class EditableRow(
    val cells: Map<String, String>
)

/**
 * Отображает таблицу в режиме редактирования.
 *
 * Колонка id показывается только для чтения, остальные колонки представлены
 * текстовыми полями. Каждая строка содержит кнопку удаления, а под таблицей
 * расположена кнопка добавления новой строки.
 *
 * Компонент не хранит состояние: все изменения передаются через колбэки.
 *
 * @param rows строки таблицы для отображения.
 * @param columns имена колонок в порядке отображения.
 * @param onCellChange вызывается при изменении значения ячейки.
 * @param onDeleteRow вызывается при нажатии кнопки удаления строки.
 * @param onAddRow вызывается при добавлении строки; null скрывает кнопку добавления.
 */
@Composable
fun EditableDatabaseTable(
    rows: List<EditableRow>,
    columns: List<String>,
    onCellChange: (rowIndex: Int, column: String, value: String) -> Unit,
    onDeleteRow: (rowIndex: Int) -> Unit,
    onAddRow: (() -> Unit)?
) {
    val idColumn = columns.firstOrNull { it.equals("id", ignoreCase = true) }
    val columnWeights = calculateEditableColumnWeights(columns, rows)

    HorizontalDivider()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (columns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Таблица пуста",
                    style = MaterialTheme.typography.titleSmallEmphasized
                )
            }
        } else {
            // Заголовок таблицы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (column in columns) {
                    Text(
                        text = column,
                        modifier = Modifier
                            .weight(columnWeights[column] ?: 1f)
                            .padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                }

                Text(
                    text = "Действия",
                    modifier = Modifier
                        .width(56.dp)
                        .padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.titleSmallEmphasized
                )
            }

            HorizontalDivider()

            // Записи таблицы
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (column in columns) {
                        // id назначается базой данных и отображается только для чтения
                        if (column == idColumn) {
                            Text(
                                text = row.cells[column].orEmpty(),
                                modifier = Modifier
                                    .weight(columnWeights[column] ?: 1f)
                                    .padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodyMediumEmphasized
                            )
                        } else {
                            OutlinedTextField(
                                value = row.cells[column].orEmpty(),
                                onValueChange = { value ->
                                    onCellChange(index, column, value)
                                },
                                modifier = Modifier
                                    .weight(columnWeights[column] ?: 1f)
                                    .padding(horizontal = 4.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMediumEmphasized
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onDeleteRow(index)
                        },
                        modifier = Modifier.width(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить"
                        )
                    }
                }

                HorizontalDivider()
            }

            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет записей",
                        style = MaterialTheme.typography.bodyMediumEmphasized
                    )
                }
            }

            if (onAddRow != null) {
                OutlinedButton(
                    onClick = onAddRow,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("Добавить строку")
                }
            }
        }
    }
}

/**
 * Вычисляет веса колонок по максимальной длине их значений.
 *
 * Используется для пропорционального распределения ширины колонок в таблице.
 *
 * @param columns имена колонок.
 * @param rows строки таблицы.
 * @return вес каждой колонки; каждая колонка получает не менее 0.05.
 */
private fun calculateEditableColumnWeights(
    columns: List<String>,
    rows: List<EditableRow>
): Map<String, Float> {
    val maxLengths = columns.associateWith { columnName ->
        columnName.length
    }.toMutableMap()

    rows.forEach { row ->
        columns.forEach { columnName ->
            val value = row.cells[columnName].orEmpty()

            maxLengths[columnName] = maxOf(
                maxLengths[columnName] ?: 0,
                value.length
            )
        }
    }

    val totalLength = maxLengths.values.sum().toFloat()

    if (totalLength == 0f) {
        return emptyMap()
    }

    return maxLengths.mapValues { (_, length) ->
        val weight = length / totalLength
        if (weight >= 0.05f) weight else 0.05f
    }
}

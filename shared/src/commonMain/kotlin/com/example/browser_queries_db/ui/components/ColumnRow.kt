package com.example.browser_queries_db.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Разрешённые типы колонок для форм создания и редактирования таблицы
val COLUMN_TYPES = listOf(
    "string",
    "int",
    "double",
    "date"
)

// Черновик колонки, используемый формами создания и редактирования таблицы
data class ColumnDraft(
    val name: String = "",
    val type: String = COLUMN_TYPES.first(),
    val nullable: Boolean = false
)

/*
 * Отображает одну колонку в форме создания/редактирования таблицы
 *
 * Содержит поля для ввода имени, выбора типа данных и настройки nullable,
 * а также опциональную кнопку удаления колонки
 */
@Composable
fun ColumnRow(
    number: Int,
    columnName: String,
    columnType: String,
    isNullable: Boolean,
    onColumnNameChange: (String) -> Unit,
    onColumnTypeChange: (String) -> Unit,
    onNullableChange: (Boolean) -> Unit,
    nameEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Секция ввода названия
            Column {
                Text("Название колонки №$number:")
                TextField(
                    value = columnName,
                    onValueChange = onColumnNameChange,
                    enabled = nameEnabled
                )
            }

            // Секция выбора типа данных
            Box {
                OutlinedButton(
                    onClick = {
                        dropdownExpanded = true
                    }
                ) {
                    Text(columnType)
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = {
                        dropdownExpanded = false
                    }
                ) {
                    for (datatype in COLUMN_TYPES) {
                        DropdownMenuItem(
                            text = {
                                Text(datatype)
                            },
                            onClick = {
                                onColumnTypeChange(datatype)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Секция поддержки null
            Text("nullable:")
            Checkbox(
                checked = isNullable,
                onCheckedChange = onNullableChange
            )
        }

        // Кнопка удаления колонки (если удаление доступно)
        if (onDelete != null) {
            TextButton(
                onClick = onDelete
            ) {
                Text("Удалить")
            }
        }
    }
}

package com.example.browser_queries_db.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

@Composable
fun FieldsDialog(
    fields: List<String>,
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit
) {

    // Поля выбранные пользователем
    var selectedFields by remember { mutableStateOf(fields.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Отображаемые поля")
        },

        text = {
            Column {
                fields.forEach { field ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            // Если поле выбрано пользователем
                            checked = field in selectedFields,
                            onCheckedChange = { checked ->
                                selectedFields =
                                    if (checked) {
                                        selectedFields + field
                                    } else {
                                        selectedFields - field
                                    }
                            }
                        )

                        Text(field)
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    // Отбираем поля в правильной последовательности. Если поле выбрано, то оно попадает в список
                    val orderedFields = fields
                        .filter { field ->
                            field in selectedFields
                        }
                        .map { field ->
                            field
                        }

                    onApply(orderedFields)
                }
            ) {
                Text("Применить")
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
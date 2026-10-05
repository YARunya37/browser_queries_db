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
    onDismiss: () -> Unit,
    onApply: (List<String>) -> Unit
) {
    val fields = listOf(
        "fio" to "ФИО",
        "phone" to "Телефон",
        "salary" to "Зарплата",
        "address" to "Адрес",
        "experience" to "Стаж"
    )

    var selectedFields by remember {
        mutableStateOf(
            setOf(
                "fio",
                "phone",
                "salary",
                "address",
                "experience"
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Отображаемые поля")
        },

        text = {
            Column {
                fields.forEach { (field, displayName) ->
                    val selected = field in selectedFields

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = {
                                selectedFields =
                                    if (it) {
                                        selectedFields + field
                                    } else {
                                        selectedFields - field
                                    }
                            }
                        )

                        Text(displayName)
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onApply(selectedFields.toList())
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
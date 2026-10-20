package com.example.browser_queries_db.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SortDialog(
    fields: List<String>,
    onDismiss: () -> Unit,
    onApply: (field: String, direction: String) -> Unit
) {
    var selectedField by remember { mutableStateOf("fio") }
    var selectedDirection by remember { mutableStateOf("asc") }

    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Сортировка")
        },

        text = {
            Column {
                Text("Поле")

                Box {
                    OutlinedButton(
                        onClick = {
                            dropdownExpanded = true
                        }
                    ) {
                        Text(
                            selectedField
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = {
                            dropdownExpanded = false
                        }
                    ) {
                        for (field in fields) {
                            DropdownMenuItem(
                                text = {
                                    Text(field)
                                },
                                onClick = {
                                    selectedField = field
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text("Направление")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedDirection == "asc",
                        onClick = {
                            selectedDirection = "asc"
                        }
                    )

                    Text("По возрастанию")

                    RadioButton(
                        selected = selectedDirection == "desc",
                        onClick = {
                            selectedDirection = "desc"
                        }
                    )

                    Text("По убыванию")
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        selectedField,
                        selectedDirection
                    )
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
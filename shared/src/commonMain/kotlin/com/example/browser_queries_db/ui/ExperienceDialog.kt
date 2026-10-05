package com.example.browser_queries_db.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ExperienceDialog(
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit
) {
    var experience by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Фильтр по стажу")
        },

        text = {
            OutlinedTextField(
                value = experience,
                onValueChange = {
                    experience = it
                },
                label = {
                    Text("Минимальный стаж")
                }
            )
        },

        confirmButton = {
            Button(
                onClick = {
                    experience.toIntOrNull()?.let {
                        onApply(it)
                    }
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
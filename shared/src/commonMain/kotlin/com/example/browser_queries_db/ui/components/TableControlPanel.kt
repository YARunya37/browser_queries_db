package com.example.browser_queries_db.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * Отображает панель управления таблицами
 *
 * Содержит в себе кнопки для создания таблицы, редактирования, удаления таблицы, выбора таблицы для отображения
 */
@Composable
fun TableControlPanel(
    onCreateTable: () -> Unit,
    onEditTable: () -> Unit,
    onDeleteTable: () -> Unit,
    onShowTable: () -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Кнопка создания таблицы
        Button(
            onClick = onCreateTable,
            ) {
            Text(text = "Создать таблицу")
        }

        // Кнопка редактирования таблицы
        Button(
            onClick = onEditTable,
        ) {
            Text(text = "Изменить таблицу")
        }

        // Кнопка удаления таблицы
        Button(
            onClick = onDeleteTable,
        ) {
            Text(text = "Удалить таблицу")
        }


        // Кнопка отображения таблицы
        Button(
            onClick = onShowTable,
        ) {
            Text(text = "Показать таблицу")
        }

    }

}
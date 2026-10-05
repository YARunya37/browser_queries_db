package com.example.browser_queries_db

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun EmployeeTable(employees: JsonArray) {

    // Определяем какие колонки получили
    val columns =
        employees.firstOrNull()
            ?.jsonObject
            ?.keys
            ?: emptySet()

    // Таблица
    HorizontalDivider()
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Заголовок таблицы
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 12.dp)
        ) {
            for (column in columns) {
                Text(
                    text = column,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        HorizontalDivider()

        // Записи таблицы
        for (employee in employees) {
            val employeeObject = employee.jsonObject

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Пишем значения для каждой колонки
                for (column in columns) {
                    val value = employeeObject[column]

                    Text(
                        text = when {
                            value == null || value is JsonNull -> ""
                            else -> value.jsonPrimitive.content
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
            HorizontalDivider()
          }
    }
}
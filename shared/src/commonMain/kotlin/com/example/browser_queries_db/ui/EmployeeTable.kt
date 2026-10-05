package com.example.browser_queries_db.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun EmployeeTable(employees: JsonArray) {

    // Названия колонок для отображения в UI
    val columnNames = mapOf(
        "id" to "ID",
        "fio" to "ФИО",
        "phone" to "Телефон",
        "salary" to "Зарплата",
        "address" to "Адрес",
        "experience" to "Стаж"
    )

    // Веса для колонок
    val columnWeights = mapOf(
        "id" to 0.5f,
        "fio" to 2f,
        "phone" to 1.5f,
        "salary" to 1f,
        "address" to 2f,
        "experience" to 0.5f
    )

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
        if (employees.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Таблица пуста",
                    style = MaterialTheme.typography.titleSmallEmphasized,
                )
            }

        }else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp)
            ) {
                for (column in columns) {
                    Text(
                        text = columnNames[column] ?: column,
                        modifier = Modifier
                            .weight(columnWeights[column] ?: 1f)
                            .padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                }
            }
        }
        HorizontalDivider()

        // Записи таблицы
        for (employee in employees) {
            val employeeObject = employee.jsonObject

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
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
                            .weight(columnWeights[column] ?: 1f)
                            .padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.bodyMediumEmphasized
                    )
                }
            }
            HorizontalDivider()
          }
    }
}
package com.example.browser_queries_db.ui.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import androidx.compose.ui.Alignment
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun DatabaseTable(rows: JsonArray) {

    // Определяем какие колонки получили
    val columns = rows.firstOrNull()?.jsonObject?.keys?.toList() ?: emptyList()

    // Веса каждой из колонок
    val columnWeights = calculateColumnWeights(columns, rows)


    // Таблица
    HorizontalDivider()
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Заголовок таблицы
        if (rows.isEmpty()) {

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

        } else {
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
                            .weight(columnWeights[column] ?: 1f)
                            .padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleSmallEmphasized
                    )
                }
            }
        }
        HorizontalDivider()

        // Записи таблицы
        for (row in rows) {
            // Объект - строка таблицы(колонка1 : знач, колонка2 : знач ...)
            val rowObject = row.jsonObject

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                // Пишем значения для каждой колонки
                for (column in columns) {
                    val value = rowObject[column]

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

fun calculateColumnWeights(
    columns: List<String>,
    rows: JsonArray
): Map<String, Float> {
    val maxLengths = columns.associateWith { columnName ->
        columnName.length
    }.toMutableMap()

    rows.forEach { row ->
        val rowObject = row.jsonObject

        columns.forEach { columnName ->

            val value = rowObject[columnName]?.let {
                if (it is JsonNull) "" else it.jsonPrimitive.content
            } ?: ""

            maxLengths[columnName] = maxOf(
                maxLengths[columnName] ?: 0,
                value.length
            )
        }
    }

    val totalLength = maxLengths.values.sum().toFloat()

    return maxLengths.mapValues { (_, length) ->
        val weight = length / totalLength
        if (weight >= 0.05f) weight else 0.05f
    }
}
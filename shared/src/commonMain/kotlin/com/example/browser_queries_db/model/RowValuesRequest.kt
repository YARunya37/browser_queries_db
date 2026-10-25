package com.example.browser_queries_db.model

import kotlinx.serialization.Serializable

/**
 * Тело запроса добавления или изменения строки таблицы.
 *
 * @property values значения колонок: имя колонки -> значение.
 *                  Пустая строка или null пропускаются.
 */
@Serializable
data class RowValuesRequest(
    val values: Map<String, String?>
)

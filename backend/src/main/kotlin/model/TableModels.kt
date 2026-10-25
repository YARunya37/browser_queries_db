package com.example.model

import kotlinx.serialization.Serializable

// Описание одной колонки таблицы, приходит в теле POST/PATCH запросов
@Serializable
data class ColumnDefinition(
    val name: String,
    val type: String,
    val nullable: Boolean = true,
    // "add" | "drop" | "alter" — используется только в PATCH
    val action: String = "add"
)

// Тело запроса создания/изменения структуры таблицы
@Serializable
data class TableColumnsRequest(
    val columns: List<ColumnDefinition>
)

/**
 * Тело запроса добавления или изменения строки таблицы.
 *
 * @property values значения колонок: имя колонки -> значение.
 *                  Пустая строка или null сохраняются в БД как NULL.
 */
@Serializable
data class RowValuesRequest(
    val values: Map<String, String?>
)

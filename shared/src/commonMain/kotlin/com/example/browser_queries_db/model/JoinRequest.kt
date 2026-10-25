package com.example.browser_queries_db.model

import kotlinx.serialization.Serializable

/**
 * Тело запроса INNER JOIN двух таблиц по одной паре колонок.
 *
 * @property firstTable первая таблица.
 * @property secondTable вторая таблица.
 * @property firstColumn колонка первой таблицы.
 * @property secondColumn колонка второй таблицы.
 */
@Serializable
data class JoinTablesRequest(
    val firstTable: String,
    val secondTable: String,
    val firstColumn: String,
    val secondColumn: String
)

package com.example.repository

import com.example.database.createConnection
import com.example.model.ColumnDefinition

class TableRepository {
    // TODO: Переименование таблицы/колонны


    /**
     * Создаёт таблицу. Первой колонкой всегда идёт id SERIAL PRIMARY KEY.
     * Имена и типы приходят уже провалидированными из TableService.
     * Выполняет CREATE TABLE в БД.
     */
    fun createTable(
        tableName: String,
        columns: List<ColumnDefinition>
    ) {

        val columnDefinitions = buildList {
            add("\"id\" SERIAL PRIMARY KEY")

            for (column in columns) {
                add(buildColumnDefinition(column))
            }
        }

        val sql = "CREATE TABLE ${quote(tableName)} (" +
                columnDefinitions.joinToString(", ") +
                ")"

        executeUpdate(sql)
    }

    /**
     * Получает структуру таблицы.
     * Имя таблицы передаётся как значение и связывается через PreparedStatement,
     * чтобы избежать SQL-инъекции.
     *
     * @return список строк: каждая строка — map имя колонки -> значение
     *         (column_name, data_type, is_nullable, column_default).
     */
    fun getTableStructure(
        tableName: String
    ): List<Map<String, Any?>> {

        val sql = """
            SELECT column_name, data_type, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ?
            ORDER BY ordinal_position
        """.trimIndent()

        createConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.setString(1, tableName)

                statement.executeQuery().use { resultSet ->

                    val metadata = resultSet.metaData
                    val columnCount = metadata.columnCount

                    return buildList {

                        while (resultSet.next()) {

                            val row = buildMap {

                                for (i in 1..columnCount) {
                                    val columnName = metadata.getColumnLabel(i)
                                    val value = resultSet.getObject(i)
                                    put(columnName, value)
                                }
                            }

                            add(row)
                        }
                    }
                }
            }
        }
    }

    /**
     * Изменяет структуру таблицы. Каждая колонка описывает одну операцию:
     * add / drop / alter. Выполняет ALTER TABLE в БД.
     */
    fun alterTable(
        tableName: String,
        columns: List<ColumnDefinition>
    ) {

        val clauses = columns.map { column ->
            when (column.action.lowercase()) {
                "add" -> "ADD COLUMN ${buildColumnDefinition(column)}"

                "drop" -> "DROP COLUMN ${quote(column.name)}"

                "alter" -> buildAlterClause(column)

                else -> throw IllegalArgumentException(
                    "Unknown action: ${column.action}"
                )
            }
        }

        val sql = "ALTER TABLE ${quote(tableName)} " +
                clauses.joinToString(", ")

        executeUpdate(sql)
    }

    /**
     * Удаляет таблицу. Выполняет DROP TABLE в БД.
     */
    fun dropTable(tableName: String) {
        val sql = "DROP TABLE ${quote(tableName)}"
        executeUpdate(sql)
    }

    /**
     * Собирает определение одной колонки для SQL: "имя" ТИП [NOT NULL].
     *
     * @return строка с определением колонки.
     */
    private fun buildColumnDefinition(
        column: ColumnDefinition
    ): String = buildString {
        append(quote(column.name))
        append(" ")
        append(column.type)

        if (!column.nullable) {
            append(" NOT NULL")
        }
    }

    /**
     * Собирает SQL-фразу изменения колонки: смена типа и установка/снятие NOT NULL.
     *
     * @return строка с фразой изменения колонки.
     */
    private fun buildAlterClause(
        column: ColumnDefinition
    ): String = buildString {
        append("ALTER COLUMN ")
        append(quote(column.name))
        append(" TYPE ")
        append(column.type)

        if (column.nullable) {
            append(", ALTER COLUMN ")
            append(quote(column.name))
            append(" DROP NOT NULL")
        } else {
            append(", ALTER COLUMN ")
            append(quote(column.name))
            append(" SET NOT NULL")
        }
    }

    /**
     * Выполняет SQL-запрос на изменение (CREATE/ALTER/DROP),
     * открывая и закрывая соединение.
     */
    private fun executeUpdate(sql: String) {
        createConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(sql)
            }
        }
    }

    /**
     * Оборачивает идентификатор (имя таблицы/колонки) в двойные кавычки.
     *
     * @return идентификатор в двойных кавычках.
     */
    private fun quote(identifier: String): String = "\"$identifier\""
}

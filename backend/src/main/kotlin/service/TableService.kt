package com.example.service

import com.example.json.toJsonArray
import com.example.model.ColumnDefinition
import com.example.repository.TableRepository
import kotlinx.serialization.json.JsonArray

class TableService(
    private val tableRepository: TableRepository
) {

    // Разрешённые типы колонок: строка -> SQL-тип
    private val typeMap = mapOf(
        "string" to "TEXT",
        "strings" to "TEXT",
        "int" to "INTEGER",
        "double" to "DOUBLE PRECISION",
        "date" to "DATE"
    )

    private val allowedActions = setOf("add", "drop", "alter")

    private val identifierRegex = Regex("^[A-Za-z_][A-Za-z0-9_]{0,62}$")

    /**
     * Создаёт таблицу с указанными колонками.
     * Проверяет имя таблицы, список колонок и переводит типы в SQL-типы.
     *
     * @throws IllegalArgumentException если данные некорректны.
     */
    fun createTable(
        tableName: String,
        columns: List<ColumnDefinition>
    ) {
        validateTableName(tableName)
        validateColumns(columns)

        val sqlColumns = columns.map { it.copy(type = mapType(it.type)) }

        tableRepository.createTable(tableName, sqlColumns)
    }

    /**
     * Возвращает структуру таблицы.
     * Проверяет имя таблицы; при некорректном имени бросает IllegalArgumentException.
     *
     * @return JsonArray, где каждый элемент — объект с именем колонки,
     *         типом, признаком nullable и значением по умолчанию.
     * @throws IllegalArgumentException если имя таблицы некорректно.
     */
    fun getTableStructure(tableName: String): JsonArray {
        validateTableName(tableName)

        return toJsonArray(tableRepository.getTableStructure(tableName))
    }

    /**
     * Изменяет структуру таблицы по списку колонок (операции add/drop/alter).
     * Проверяет имя таблицы, колонки, действия и переводит типы в SQL-типы.
     *
     * @throws IllegalArgumentException если данные некорректны.
     */
    fun alterTable(
        tableName: String,
        columns: List<ColumnDefinition>
    ) {
        validateTableName(tableName)

        if (columns.isEmpty()) {
            throw IllegalArgumentException("Columns cannot be empty")
        }

        validateUniqueNames(columns)

        val sqlColumns = columns.map { column ->
            validateColumnName(column.name)
            validateAction(column.action, column.name)

            // У id менять тип/удалять нельзя
            if (column.action.lowercase() in setOf("drop", "alter")) {
                throw IllegalArgumentException(
                    "Column id cannot be dropped or altered"
                )
            }

            column.copy(type = mapType(column.type))
        }

        tableRepository.alterTable(tableName, sqlColumns)
    }

    /**
     * Удаляет таблицу из БД.
     * Проверяет имя таблицы.
     *
     * @throws IllegalArgumentException если имя таблицы некорректно.
     */
    fun dropTable(tableName: String) {
        validateTableName(tableName)

        tableRepository.dropTable(tableName)
    }

    /**
     * Проверяет имя таблицы на допустимые символы.
     *
     * @throws IllegalArgumentException если имя таблицы недопустимо.
     */
    private fun validateTableName(tableName: String) {
        if (!identifierRegex.matches(tableName)) {
            throw IllegalArgumentException(
                "Invalid table name: $tableName"
            )
        }
    }

    /**
     * Проверяет список колонок при создании таблицы: непустой, уникальные имена,
     * допустимые имена и типы, отсутствие колонки id (создаётся автоматически).
     *
     * @throws IllegalArgumentException если данные некорректны.
     */
    private fun validateColumns(columns: List<ColumnDefinition>) {
        if (columns.isEmpty()) {
            throw IllegalArgumentException("Columns cannot be empty")
        }

        validateUniqueNames(columns)

        for (column in columns) {
            validateColumnName(column.name)

            // id создаётся автоматически, поэтому его нельзя передавать
            if (column.name.equals("id", ignoreCase = true)) {
                throw IllegalArgumentException(
                    "Column id is created automatically"
                )
            }

            mapType(column.type)
        }
    }

    /**
     * Проверяет имя колонки на допустимые символы.
     *
     * @throws IllegalArgumentException если имя колонки недопустимо.
     */
    private fun validateColumnName(name: String) {
        if (!identifierRegex.matches(name)) {
            throw IllegalArgumentException("Invalid column name: $name")
        }
    }

    /**
     * Проверяет, что имена колонок уникальны (без учёта регистра).
     *
     * @throws IllegalArgumentException если есть повторяющиеся имена.
     */
    private fun validateUniqueNames(columns: List<ColumnDefinition>) {
        val names = columns.map { it.name.lowercase() }

        if (names.size != names.toSet().size) {
            throw IllegalArgumentException(
                "Column names must be unique"
            )
        }
    }

    /**
     * Проверяет, что действие над колонкой входит в add/drop/alter.
     *
     * @throws IllegalArgumentException если действие неизвестно.
     */
    private fun validateAction(action: String, name: String) {
        if (action.lowercase() !in allowedActions) {
            throw IllegalArgumentException(
                "Unknown action for column $name: $action"
            )
        }
    }

    /**
     * Переводит пользовательский тип колонки в SQL-тип.
     *
     * @return SQL-тип, например "TEXT".
     * @throws IllegalArgumentException если тип неизвестен.
     */
    private fun mapType(type: String): String {
        return typeMap[type.lowercase()]
            ?: throw IllegalArgumentException("Unknown type: $type")
    }
}

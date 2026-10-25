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

    private val protectedTables = setOf("table_kliuchikov")

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
     * Возвращает список всех таблиц схемы public.
     * Системные таблицы и представления не возвращаются.
     *
     * @return JsonArray, где каждый элемент — объект с именем таблицы.
     */
    fun getTableNames(): JsonArray {
        return toJsonArray(
            tableRepository.getTableNames().map { tableName ->
                mapOf("table_name" to tableName)
            }
        )
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

        validateColumns(columns)

        val sqlColumns = columns.map { column ->
            validateAction(column.action, column.name)

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

        if (tableName.lowercase() in protectedTables) {
            throw IllegalArgumentException(
                "Table '$tableName' cannot be deleted"
            )
        }

        tableRepository.dropTable(tableName)
    }

    /**
     * Читает данные из произвольной таблицы по выбранным полям.
     * Колонки валидируются по реальной структуре таблицы, полученной из БД.
     *
     * @param fields список полей через запятую; null или пусто — все поля.
     * @param sort поле сортировки; null — без сортировки.
     * @param direction направление сортировки ("asc"/"desc"); null — по умолчанию ASC.
     * @return JsonArray, где каждый элемент — объект с выбранными полями и их значениями.
     * @throws IllegalArgumentException если таблица или поля некорректны.
     */
    fun getRows(
        tableName: String,
        fields: String?,
        sort: String?,
        direction: String?
    ): JsonArray {

        validateTableName(tableName)

        // Получаем реальные колонки таблицы, чтобы валидировать выбираемые поля
        val columns = tableRepository.getColumnNames(tableName)

        if (columns.isEmpty()) {
            throw IllegalArgumentException("Table not found: $tableName")
        }

        val sqlFields = validateSelectFields(fields, columns)

        val sqlSort = validateSelectSort(sort, columns)

        val sqlDirection = validateSelectDirection(direction, sqlSort)

        return toJsonArray(
            tableRepository.selectRows(
                tableName = tableName,
                fields = sqlFields,
                sort = sqlSort,
                direction = sqlDirection
            )
        )
    }

    /**
     * Добавляет строку в таблицу.
     *
     * Проверяет имя таблицы, наличие таблицы в БД и имена колонок.
     * Пустые строки и null сохраняются в БД как NULL.
     *
     * @param values значения колонок: имя -> значение.
     * @throws IllegalArgumentException если таблица или колонка некорректны.
     */
    fun addRow(
        tableName: String,
        values: Map<String, String?>
    ) {
        validateTableName(tableName)

        val structure = getValidatedTableStructure(tableName)

        val normalizedValues = validateRowValues(values, structure)

        tableRepository.insertRow(tableName, normalizedValues)
    }

    /**
     * Изменяет строку таблицы по идентификатору.
     *
     * Проверяет имя таблицы, идентификатор, наличие таблицы и имена колонок.
     * Пустые строки и null сохраняются в БД как NULL.
     * Колонку id изменять нельзя.
     *
     * @param id идентификатор изменяемой строки.
     * @param values изменяемые колонки: имя -> новое значение.
     * @throws IllegalArgumentException если данные некорректны.
     * @throws NoSuchElementException если строка с указанным id не найдена.
     */
    fun updateRow(
        tableName: String,
        id: Long,
        values: Map<String, String?>
    ) {
        validateTableName(tableName)
        validateRowId(id)

        if (values.isEmpty()) {
            throw IllegalArgumentException("$id: Values cannot be empty")
        }

        val structure = getValidatedTableStructure(tableName)

        val normalizedValues = validateRowValues(values, structure)

        val updatedRows = tableRepository.updateRow(
            tableName = tableName,
            id = id,
            values = normalizedValues
        )

        if (updatedRows == 0) {
            throw NoSuchElementException("Row not found: $id")
        }
    }

    /**
     * Удаляет строку таблицы по идентификатору.
     *
     * Проверяет имя таблицы, идентификатор и наличие таблицы в БД.
     *
     * @param id идентификатор удаляемой строки.
     * @throws IllegalArgumentException если имя таблицы или id некорректны.
     * @throws NoSuchElementException если строка с указанным id не найдена.
     */
    fun deleteRow(
        tableName: String,
        id: Long
    ) {
        validateTableName(tableName)
        validateRowId(id)

        getValidatedTableStructure(tableName)

        val deletedRows = tableRepository.deleteRow(tableName, id)

        if (deletedRows == 0) {
            throw NoSuchElementException("Row not found: $id")
        }
    }

    /**
     * Возвращает структуру таблицы или бросает исключение, если таблица не найдена.
     *
     * @return список колонок с метаданными из information_schema.
     * @throws IllegalArgumentException если таблица не найдена.
     */
    private fun getValidatedTableStructure(
        tableName: String
    ): List<Map<String, Any?>> {
        val structure = tableRepository.getTableStructure(tableName)

        if (structure.isEmpty()) {
            throw IllegalArgumentException("Table not found: $tableName")
        }

        return structure
    }

    /**
     * Проверяет и нормализует значения колонок строки.
     *
     * Имена колонок сопоставляются с реальными именами без учёта регистра.
     * Колонка id запрещена, неизвестные и повторяющиеся колонки отклоняются.
     *
     * @return значения с реальными именами колонок; пустая строка заменена на null.
     * @throws IllegalArgumentException если данные некорректны.
     */
    private fun validateRowValues(
        values: Map<String, String?>,
        structure: List<Map<String, Any?>>
    ): Map<String, String?> {
        val columns = structure.mapNotNull { column ->
            column["column_name"] as? String
        }

        val normalizedValues = linkedMapOf<String, String?>()

        for ((name, value) in values) {
            val realName = resolveColumn(name, columns)

            if (realName.equals("id", ignoreCase = true)) {
                throw IllegalArgumentException(
                    "Column id cannot be changed"
                )
            }

            if (normalizedValues.containsKey(realName)) {
                throw IllegalArgumentException(
                    "Duplicate column: $realName"
                )
            }

            normalizedValues[realName] = value?.takeIf { it.isNotEmpty() }
        }

        return normalizedValues
    }

    /**
     * Проверяет, что идентификатор строки положительный.
     *
     * @throws IllegalArgumentException если id некорректен.
     */
    private fun validateRowId(id: Long) {
        if (id <= 0) {
            throw IllegalArgumentException("Invalid row id: $id")
        }
    }

    /**
     * Проверяет выбираемые поля по реальным колонкам таблицы (без учёта регистра).
     *
     * @return список реальных имён колонок; пустой список означает все колонки (*).
     * @throws IllegalArgumentException если поле отсутствует в таблице.
     */
    private fun validateSelectFields(
        fields: String?,
        columns: List<String>
    ): List<String> {

        if (fields.isNullOrBlank()) {
            return emptyList()
        }

        return fields.split(",").map { field ->
            resolveColumn(field.trim(), columns)
        }
    }

    /**
     * Проверяет поле сортировки по реальным колонкам таблицы.
     *
     * @return реальное имя колонки сортировки или null.
     * @throws IllegalArgumentException если поле отсутствует в таблице.
     */
    private fun validateSelectSort(
        sort: String?,
        columns: List<String>
    ): String? {
        return sort?.let { resolveColumn(it.trim(), columns) }
    }

    /**
     * Проверяет и нормализует направление сортировки.
     *
     * @return "ASC"/"DESC" или null, если сортировка не задана.
     * @throws IllegalArgumentException если направление некорректно или задано без сортировки.
     */
    private fun validateSelectDirection(
        direction: String?,
        sqlSort: String?
    ): String? {

        var sqlDirection: String? = null

        if (direction != null) {
            sqlDirection = when (direction.lowercase()) {
                "asc" -> "ASC"
                "desc" -> "DESC"

                else -> throw IllegalArgumentException(
                    "Direction must be ASC or DESC"
                )
            }
        }

        // direction имеет смысл только вместе с sort
        if (sqlDirection != null && sqlSort == null) {
            throw IllegalArgumentException("Direction requires sort")
        }

        // Значение по умолчанию, если направление не указано
        if (sqlSort != null && sqlDirection == null) {
            sqlDirection = "ASC"
        }

        return sqlDirection
    }

    /**
     * Находит реальное имя колонки по имени из запроса (без учёта регистра).
     *
     * @return реальное имя колонки.
     * @throws IllegalArgumentException если колонка не найдена.
     */
    private fun resolveColumn(
        name: String,
        columns: List<String>
    ): String {
        return columns.firstOrNull { it.equals(name, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unknown field: $name")
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

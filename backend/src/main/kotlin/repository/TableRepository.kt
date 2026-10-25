package com.example.repository

import com.example.database.createConnection
import com.example.model.ColumnDefinition
import java.sql.PreparedStatement

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
     * Получает список имён колонок таблицы.
     * Имя таблицы передаётся как значение через PreparedStatement,
     * чтобы избежать SQL-инъекции.
     *
     * @return список имён колонок в порядке их следования в таблице.
     */
    fun getColumnNames(tableName: String): List<String> {

        val sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ?
            ORDER BY ordinal_position
        """.trimIndent()

        createConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.setString(1, tableName)

                statement.executeQuery().use { resultSet ->

                    return buildList {

                        while (resultSet.next()) {
                            add(resultSet.getString("column_name"))
                        }
                    }
                }
            }
        }
    }

    /**
     * Получает список всех таблиц схемы public.
     * Системные таблицы и представления не возвращаются.
     *
     * @return список имён таблиц в алфавитном порядке.
     */
    fun getTableNames(): List<String> {

        val sql = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
            ORDER BY table_name
        """.trimIndent()

        createConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

                statement.executeQuery().use { resultSet ->

                    return buildList {

                        while (resultSet.next()) {
                            add(resultSet.getString("table_name"))
                        }
                    }
                }
            }
        }
    }

    /**
     * Читает данные из таблицы с динамическим набором колонок.
     * Имена таблицы и колонок приходят уже провалидированными из TableService
     * и оборачиваются в кавычки; значения сортировки не подставляются извне.
     *
     * @param fields список колонок для выборки; пустой список означает все колонки (*).
     * @param sort колонка сортировки или null.
     * @param direction направление сортировки ("ASC"/"DESC") или null.
     * @return список строк: каждая строка — map имя колонки -> значение.
     */
    fun selectRows(
        tableName: String,
        fields: List<String>,
        sort: String?,
        direction: String?
    ): List<Map<String, Any?>> {

        val sqlFields = if (fields.isEmpty()) {
            "*"
        } else {
            fields.joinToString(", ") { quote(it) }
        }

        val sql = buildString {
            append("SELECT ")
            append(sqlFields)
            append(" FROM ")
            append(quote(tableName))

            if (sort != null) {
                append(" ORDER BY ")
                append(quote(sort))
                append(" ")
                append(direction)
            }
        }

        createConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

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
     * Выполняет INNER JOIN двух таблиц по одной паре колонок.
     *
     * Имена таблиц и колонок приходят уже провалидированными из TableService
     * и оборачиваются в кавычки. Каждая колонка результата получает алиас
     * "имя_таблицы.имя_колонки", чтобы избежать конфликта одинаковых имён
     * (например, id в обеих таблицах).
     *
     * @param firstColumns все колонки первой таблицы.
     * @param secondColumns все колонки второй таблицы.
     * @param firstColumn колонка первой таблицы для условия соединения.
     * @param secondColumn колонка второй таблицы для условия соединения.
     * @return список строк: каждая строка — map имя колонки -> значение.
     */
    fun selectInnerJoin(
        firstTable: String,
        secondTable: String,
        firstColumns: List<String>,
        secondColumns: List<String>,
        firstColumn: String,
        secondColumn: String
    ): List<Map<String, Any?>> {

        val sqlFields = buildList {
            for (column in firstColumns) {
                add("t1.${quote(column)} AS ${quote("$firstTable.$column")}")
            }

            for (column in secondColumns) {
                add("t2.${quote(column)} AS ${quote("$secondTable.$column")}")
            }
        }.joinToString(", ")

        val sql = "SELECT $sqlFields " +
                "FROM ${quote(firstTable)} AS t1 " +
                "INNER JOIN ${quote(secondTable)} AS t2 " +
                "ON t1.${quote(firstColumn)} = t2.${quote(secondColumn)}"

        createConnection().use { connection ->

            connection.prepareStatement(sql).use { statement ->

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
     * Добавляет строку в таблицу.
     *
     * Имена колонок приходят уже провалидированными из TableService.
     * Значения передаются через PreparedStatement и приводятся к типу колонки
     * на стороне БД, поэтому корректно сохраняется текст на русском языке.
     *
     * @param values значения колонок: имя -> значение; пустая карта означает
     *               вставку строки со значениями по умолчанию.
     */
    fun insertRow(
        tableName: String,
        values: Map<String, String?>
    ) {
        val columnTypes = getColumnTypes(tableName)

        val sql = if (values.isEmpty()) {
            "INSERT INTO ${quote(tableName)} DEFAULT VALUES"
        } else {
            val columns = values.keys.toList()

            val quotedColumns = columns.joinToString(", ") { quote(it) }

            val placeholders = columns.joinToString(", ") { column ->
                buildCastPlaceholder(column, columnTypes)
            }

            "INSERT INTO ${quote(tableName)} ($quotedColumns) " +
                    "VALUES ($placeholders)"
        }

        executePreparedUpdate(sql) { statement ->
            bindValues(statement, values)
        }
    }

    /**
     * Изменяет строку таблицы по идентификатору.
     *
     * Значения передаются через PreparedStatement и приводятся к типу колонки
     * на стороне БД, что обеспечивает поддержку русского текста.
     *
     * @param id идентификатор изменяемой строки.
     * @param values изменяемые колонки: имя -> новое значение.
     * @return количество изменённых строк (0, если строка не найдена).
     * @throws IllegalArgumentException если список значений пуст.
     */
    fun updateRow(
        tableName: String,
        id: Long,
        values: Map<String, String?>
    ): Int {
        if (values.isEmpty()) {
            throw IllegalArgumentException("Values cannot be empty")
        }

        val columnTypes = getColumnTypes(tableName)

        val assignments = values.keys.joinToString(", ") { column ->
            "${quote(column)} = ${buildCastPlaceholder(column, columnTypes)}"
        }

        val sql = "UPDATE ${quote(tableName)} " +
                "SET $assignments " +
                "WHERE ${quote("id")} = ?"

        return executePreparedUpdate(sql) { statement ->
            val nextIndex = bindValues(statement, values)
            statement.setLong(nextIndex, id)
        }
    }

    /**
     * Удаляет строку таблицы по идентификатору.
     *
     * @param id идентификатор удаляемой строки.
     * @return количество удалённых строк (0, если строка не найдена).
     */
    fun deleteRow(
        tableName: String,
        id: Long
    ): Int {
        val sql = "DELETE FROM ${quote(tableName)} " +
                "WHERE ${quote("id")} = ?"

        return executePreparedUpdate(sql) { statement ->
            statement.setLong(1, id)
        }
    }

    /**
     * Возвращает соответствие имени колонки и её SQL-типа,
     * полученное из существующей структуры таблицы.
     *
     * @return map имя колонки -> SQL-тип.
     */
    private fun getColumnTypes(tableName: String): Map<String, String> {
        return getTableStructure(tableName).associate { column ->
            val name = column["column_name"] as String
            val type = column["data_type"] as String

            name to type
        }
    }

    /**
     * Собирает placeholder с приведением к типу колонки: CAST(? AS ТИП).
     *
     * @throws IllegalArgumentException если колонка отсутствует в таблице.
     */
    private fun buildCastPlaceholder(
        column: String,
        columnTypes: Map<String, String>
    ): String {
        val type = columnTypes[column]
            ?: throw IllegalArgumentException("Unknown column: $column")

        return "CAST(? AS $type)"
    }

    /**
     * Связывает значения с параметрами PreparedStatement в порядке колонок.
     *
     * @return индекс следующего свободного параметра.
     * @throws IllegalArgumentException если колонка отсутствует в таблице.
     */
    private fun bindValues(
        statement: PreparedStatement,
        values: Map<String, String?>,
    ): Int {
        var index = 1

        for ((_, value) in values) {
            // В запросе собирается приведение типа: CAST(? AS TYPE).
            // Передаём в качестве параметра запроса значение
            statement.setString(index, value)
            index++
        }

        return index
    }

    /**
     * Выполняет PreparedStatement-запрос на изменение данных.
     *
     * @param bind связывает значения с параметрами запроса.
     * @return количество изменённых строк.
     */
    private fun executePreparedUpdate(
        sql: String,
        bind: (PreparedStatement) -> Unit
    ): Int {
        createConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                bind(statement)

                return statement.executeUpdate()
            }
        }
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
        append(" USING ")
        append(quote(column.name))
        append("::")
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

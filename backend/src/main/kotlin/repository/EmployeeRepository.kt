package com.example.repository

import com.example.database.createConnection

class EmployeeRepository {
    // Метод выполнения запроса с БД. Возвращает список результатов запросов в формате: имя - значение
    fun findEmployees(
        sqlRequest: String,
        experience: Int? = null
    ): List<Map<String, Any?>> {

        // Соединение с БД
        // use автоматически закроет соединение после завершения работы
        // .use обязательно у каждого JDBC объекта!!!
        createConnection().use { connection ->

            // Подготавливаем запрос
            connection.prepareStatement(sqlRequest).use { statement ->

                // Если указан опыт, то передаём параметр в запрос SQL
                if (experience != null) {
                    statement.setInt(1, experience)
                }

                // Выполняем запрос и получаем результат
                statement.executeQuery().use { resultSet ->
                    // Собираем json-ы
                    val metadata = resultSet.metaData
                    val columnCount = metadata.columnCount

                    return buildList {

                        while (resultSet.next()) {

                            val row = buildMap {

                                for (i in 1..columnCount) {
                                    val columnName =
                                        metadata.getColumnLabel(i)

                                    val value =
                                        resultSet.getObject(i)

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
}
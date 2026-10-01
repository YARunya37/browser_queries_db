package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        // Маршрут для SELECT запросов в БД.
        // Необходимые данные для формирования запроса передаются через параметры http
        get("/employees") {

            val fields = call.request.queryParameters["fields"]
            val sort = call.request.queryParameters["sort"]
            val direction = call.request.queryParameters["direction"]
            val experienceMin =
                call.request.queryParameters["experienceMin"]

            // TODO
            // После успешной реализации разделить ответственность на Repository, Service

            val allowedFields = mapOf(
                "id" to "id",
                "fio" to "fio",
                "phone" to "phone",
                "salary" to "salary",
                "address" to "address",
                "experience" to "experience"
            )


            // Формирование выбранных полей для запроса
            // Если указаны определенные поля, то указываем только их. Иначе - все поля
            val sqlFields: List<String>

            if (fields != null) {

                val requestedFields = fields.split(",")

                val result = mutableListOf<String>()

                for (field in requestedFields) {

                    val fieldName = field.trim()

                    if (!allowedFields.containsKey(fieldName)) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Unknown field: $fieldName"
                        )
                        return@get
                    }

                    result.add(allowedFields[fieldName]!!)
                }

                if (result.isEmpty()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Fields cannot be empty"
                    )
                    return@get
                }

                sqlFields = result

            } else {
                sqlFields = listOf("*")
            }


            // Сортировка по определённому полю
            val sqlSort = sort?.let {
                allowedFields[it] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "Unknown sort field: $it"
                    )
                    return@get
                }
            }


            // Направление сортировки
            var sqlDirection: String? = null

            if (direction != null) {

                sqlDirection = when (direction.lowercase()) {
                    "asc" -> "ASC"
                    "desc" -> "DESC"
                    else -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Direction must be ASC or DESC"
                        )
                        return@get
                    }
                }
            }

            // direction имеет смысл только вместе с sort
            if (sqlDirection != null && sqlSort == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Direction requires sort"
                )
                return@get
            }
            // Значение по умолчанию, если направление не указано
            else if (sqlSort != null && sqlDirection == null) {
                sqlDirection = "ASC"
            }

            // Фильтрация по минимальному опыту
            var experience: Int? = null

            if (experienceMin != null) {

                experience = experienceMin.toIntOrNull()

                if (experience == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "experienceMin must be an integer"
                    )
                    return@get
                }

                if (experience < 0) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        "experienceMin must be >= 0"
                    )
                    return@get
                }
            }

            // Собираем SQL-запрос
            val sqlRequest = buildString {
                append("SELECT ")
                append(sqlFields.joinToString(", "))
                append(" FROM table_kliuchikov")

                if (experience != null) {
                    // Значение опыта передаём через параметр SQL-запроса
                    append(" WHERE experience > ?")
                }

                if (sqlSort != null) {
                    append(" ORDER BY ")
                    append(sqlSort)

                    if (sqlDirection != null) {
                        append(" ")
                        append(sqlDirection)
                    }
                }
            }


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

                        val rows = buildList {
                            while (resultSet.next()) {

                                val row = buildMap {
                                    for (i in 1..columnCount) {
                                        val columnName = metadata.getColumnLabel(i)
                                        val value = resultSet.getObject(i)

                                        put(
                                            columnName,
                                            toJsonElement(value)
                                        )
                                    }
                                }

                                add(JsonObject(row))
                            }

                        }

                        // Отправляем ответ от БД в json
                        call.respond(JsonArray(rows))
                    }

                }


            }
        }
    }
}

// Функция для корректного приведения типов внутри json
fun toJsonElement(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull

        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)

        else -> JsonPrimitive(value.toString())
    }
}
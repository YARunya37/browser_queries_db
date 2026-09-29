package com.example

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
            // 1. проверить fields на допустимые поля
            // 2. проверить sort на допустимые варианты сортировок
            // 3. проверить direction
            // 4. получить experienceMin
            // 5. сформировать SQL
            // 6. выполнить SQL
            // 7. вернуть JSON
            // После успешной реализации разделить отвественность на Repository, Service
        }
    }
}
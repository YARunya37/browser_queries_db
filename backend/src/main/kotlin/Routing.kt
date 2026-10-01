package com.example

import com.example.service.EmployeeService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(employeeService: EmployeeService) {
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

            val experienceMin = call.request.queryParameters["experienceMin"]

            try {

                val employees = employeeService.getEmployees(
                    fields = fields,
                    sort = sort,
                    direction = direction,
                    experienceMin = experienceMin
                )

                call.respond(employees)

            } catch (e: IllegalArgumentException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    e.message ?: "Bad request"
                )
            }
        }
    }
}
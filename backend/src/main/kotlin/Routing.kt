package com.example

import com.example.model.TableColumnsRequest
import com.example.service.EmployeeService
import com.example.service.TableService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.SQLException

fun Application.configureRouting(
    employeeService: EmployeeService,
    tableService: TableService
) {
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

        // Базовый маршрут для DDL-команд над таблицей.
        // {table_name} — имя таблицы, над которой выполняется операция
        route("/tables/{table_name}") {

            // CREATE — создание таблицы по списку колонок из тела запроса.
            // Первая колонка id SERIAL PRIMARY KEY создаётся автоматически
            post {

                val tableName = call.parameters["table_name"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                val request = call.receive<TableColumnsRequest>()

                try {

                    tableService.createTable(
                        tableName = tableName,
                        columns = request.columns
                    )

                    call.respond(
                        HttpStatusCode.Created,
                        "Table '$tableName' created"
                    )

                } catch (e: IllegalArgumentException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Bad request"
                    )

                } catch (e: SQLException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Database error"
                    )
                }
            }

            // READ — получение структуры таблицы
            get {

                val tableName = call.parameters["table_name"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                try {

                    val structure = tableService.getTableStructure(tableName)

                    call.respond(structure)

                } catch (e: IllegalArgumentException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Bad request"
                    )

                } catch (e: SQLException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Database error"
                    )
                }
            }

            // UPDATE — изменение структуры таблицы (колонки из тела запроса)
            patch {

                val tableName = call.parameters["table_name"]
                    ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                val request = call.receive<TableColumnsRequest>()

                try {

                    tableService.alterTable(
                        tableName = tableName,
                        columns = request.columns
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        "Table '$tableName' altered"
                    )

                } catch (e: IllegalArgumentException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Bad request"
                    )

                } catch (e: SQLException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Database error"
                    )
                }
            }

            // DELETE — удаление таблицы
            delete {

                val tableName = call.parameters["table_name"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                try {

                    tableService.dropTable(tableName)

                    call.respond(HttpStatusCode.NoContent)

                } catch (e: IllegalArgumentException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Bad request"
                    )

                } catch (e: SQLException) {

                    call.respond(
                        HttpStatusCode.BadRequest,
                        e.message ?: "Database error"
                    )
                }
            }
        }
    }
}
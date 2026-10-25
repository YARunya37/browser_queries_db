package com.example

import com.example.model.RowValuesRequest
import com.example.model.TableColumnsRequest
import com.example.service.TableService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.SQLException

fun Application.configureRouting(
    tableService: TableService
) {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }

        // READ — получение списка всех таблиц
        get("/tables") {

            try {

                val tables = tableService.getTableNames()

                call.respond(tables)

            } catch (e: SQLException) {

                call.respond(
                    HttpStatusCode.BadRequest,
                    e.message ?: "Database error"
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

            // SELECT — получение данных из таблицы.
            // Поля, сортировка и направление передаются через параметры http
            get("/rows") {

                val tableName = call.parameters["table_name"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                val fields = call.request.queryParameters["fields"]

                val sort = call.request.queryParameters["sort"]

                val direction = call.request.queryParameters["direction"]

                try {

                    val rows = tableService.getRows(
                        tableName = tableName,
                        fields = fields,
                        sort = sort,
                        direction = direction
                    )

                    call.respond(rows)

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

            // CREATE — добавление строки в таблицу.
            // Тело запроса: значения колонок, пустые значения пропускаются
            post("/rows") {

                val tableName = call.parameters["table_name"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                val request = call.receive<RowValuesRequest>()

                try {

                    tableService.addRow(
                        tableName = tableName,
                        values = request.values
                    )

                    call.respond(
                        HttpStatusCode.Created,
                        "Row added to '$tableName'"
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

            // UPDATE — изменение строки по её id.
            // Тело запроса: значения колонок, пустые значения пропускаются
            patch("/rows/{row_id}") {

                val tableName = call.parameters["table_name"]
                    ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                val rowId = call.parameters["row_id"]?.toLongOrNull()
                    ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        "row_id must be a number"
                    )

                val request = call.receive<RowValuesRequest>()

                try {

                    tableService.updateRow(
                        tableName = tableName,
                        id = rowId,
                        values = request.values
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        "Row updated in '$tableName'"
                    )

                } catch (e: NoSuchElementException) {

                    call.respond(
                        HttpStatusCode.NotFound,
                        e.message ?: "Row not found"
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

            // DELETE — удаление строки по её id
            delete("/rows/{row_id}") {

                val tableName = call.parameters["table_name"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        "table_name is required"
                    )

                val rowId = call.parameters["row_id"]?.toLongOrNull()
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        "row_id must be a number"
                    )

                try {

                    tableService.deleteRow(
                        tableName = tableName,
                        id = rowId
                    )

                    call.respond(HttpStatusCode.NoContent)

                } catch (e: NoSuchElementException) {

                    call.respond(
                        HttpStatusCode.NotFound,
                        e.message ?: "Row not found"
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
        }
    }
}
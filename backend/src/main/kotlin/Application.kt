package com.example

import com.example.repository.EmployeeRepository
import com.example.service.EmployeeService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS

fun Application.rootModule() {
    // Устанавливаем, чтобы можно было возвращать в качестве ответа json
    install(ContentNegotiation){
        json()
    }


    // CORS — это механизм, с помощью которого сервер явно сообщает браузеру, каким другим origins(протокол + host + port) разрешено читать его ответы.
    // Frontend и backend работают на разных портах,
    // поэтому браузер считает запрос cross-origin.
    // Разрешаем frontend с localhost:8081 обращаться к API.
    install(CORS) {
        allowHost("localhost:8081")
    }

    // DI
    val employeeRepository = EmployeeRepository()
    val employeeService = EmployeeService(employeeRepository)

    configureRouting(employeeService)
}

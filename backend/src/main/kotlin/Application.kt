package com.example

import com.example.repository.EmployeeRepository
import com.example.service.EmployeeService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.rootModule() {
    // Устанавливаем, чтобы можно было возвращать в качестве ответа json
    install(ContentNegotiation){
        json()
    }

    // DI
    val employeeRepository = EmployeeRepository()
    val employeeService = EmployeeService(employeeRepository)

    configureRouting(employeeService)
}

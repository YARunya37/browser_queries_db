package com.example

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

fun Application.rootModule() {
    // Устанавливаем, чтобы можно было возвращать в качестве ответа json
    install(ContentNegotiation){
        json()
    }

    configureRouting()
}

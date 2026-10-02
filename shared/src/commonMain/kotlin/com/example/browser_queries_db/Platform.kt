package com.example.browser_queries_db

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
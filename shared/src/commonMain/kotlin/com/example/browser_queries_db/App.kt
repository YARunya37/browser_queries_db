package com.example.browser_queries_db

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
@Preview
fun App() {
    MaterialTheme {
        var employees by remember { mutableStateOf<JsonArray?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        // Unit здесь означает: запустить один раз для данного экземпляра App.
        LaunchedEffect(Unit) {
            try {
                employees = loadEmployees()
            } catch (e: Exception) {
                error = e.message
            }
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                error != null -> {
                    Text("Error: $error")
                }

                employees == null -> {
                    Text("Loading...")
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (employee in employees) {
                            // Т. к. знаем, что каждый член jsonArray - object, то смело приводим к нему
                            EmployeeRow(employee.jsonObject)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeRow(employee: JsonObject) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        for ((name, value) in employee) {
            Text("$name: ${value.jsonPrimitive.content}")
        }
    }
}
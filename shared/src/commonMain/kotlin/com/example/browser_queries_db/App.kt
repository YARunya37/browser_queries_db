package com.example.browser_queries_db

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun App() {
    MaterialTheme {
        var employees by remember { mutableStateOf<JsonArray?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scrollState = rememberScrollState()

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
                .fillMaxSize()
                .verticalScroll(scrollState),
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
                    // Конпки запросов
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Button(
                            onClick = {
                                // TODO
                            }
                        ) {
                            Text("Зарплата сотрудников")
                        }

                        Button(
                            onClick = {
                                // TODO
                            }
                        ) {
                            Text("Отсортировать по адресу")
                        }

                        Button(
                            onClick = {
                                // TODO
                            }
                        ) {
                            Text("Стаж от 4 лет")
                        }
                    }

                    // Kotlin не гарантирует, что employees не изменится между проверками(т. к. var)
                    val employeeData = employees!!

                    // Заголовок таблицы
                    Text(text = "Таблица: Сотрудники",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // Таблица
                    EmployeeTable(employeeData)
                }
            }
        }
    }
}

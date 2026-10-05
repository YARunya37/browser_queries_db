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
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        var employees by remember { mutableStateOf<JsonArray?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scrollState = rememberScrollState()
        var sortDialogVisible by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // Локальная функция для вызова запросов
        suspend fun executeQuery(query: EmployeeQuery) {
            employees = null
            try {
                employees = loadEmployees(query)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }

        // Unit здесь означает: запустить один раз для данного экземпляра App.
        LaunchedEffect(Unit) {
            try {
                employees = loadEmployees(EmployeeQuery())
            } catch (e: Exception) {
                error = e.message
            }
        }

        // Отображение диалога сортировки
        if (sortDialogVisible) {
            SortDialog(
                onDismiss = {
                    sortDialogVisible = false
                },
                onApply = { field, direction ->
                    sortDialogVisible = false

                    scope.launch {
                        executeQuery(
                            EmployeeQuery(
                                sort = field,
                                direction = direction
                            )
                        )
                    }
                }
            )
        }

        // Контент экрана
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Кнопки запросов
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            executeQuery(
                                EmployeeQuery(
                                    fields = listOf("fio", "phone", "salary")
                                )
                            )
                        }
                    }
                ) {
                    Text("Зарплата сотрудников")
                }

                Button(
                    onClick = {
                        scope.launch {
                            executeQuery(
                                EmployeeQuery(
                                    fields = listOf("fio", "address"),
                                    sort = "address"
                                )
                            )
                        }
                    }
                ) {
                    Text("Отсортировать по адресу")
                }

                Button(
                    onClick = {
                        scope.launch {
                            executeQuery(
                                EmployeeQuery(
                                    fields = listOf("fio", "address", "experience"),
                                    experienceMin = 4
                                )
                            )
                        }
                    }
                ) {
                    Text("Стаж от 4 лет")
                }
            }

            // Заголовок таблицы
            Text(text = "Таблица: Сотрудники",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Панель управления таблицей
            Button(
                onClick = {
                    sortDialogVisible = true
                }
            ) {
                Text("Сортировка")
            }

            // Отображаем состояние таблицы
            when {
                error != null -> {
                    Text("Error: $error")
                }

                employees == null -> {
                    Text("Loading...")
                }

                else -> {

                    // Kotlin не гарантирует, что employees не изменится между проверками(т. к. var)
                    val employeeData = employees!!

                    // Таблица
                    EmployeeTable(employeeData)
                }
            }
        }
    }
}



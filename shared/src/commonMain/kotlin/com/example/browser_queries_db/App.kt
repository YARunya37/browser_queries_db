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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import kotlinx.serialization.json.JsonArray
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.browser_queries_db.model.EmployeeQuery
import com.example.browser_queries_db.ui.EmployeeTable
import com.example.browser_queries_db.ui.SortDialog
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        var employees by remember { mutableStateOf<JsonArray?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        val scrollState = rememberScrollState()

        var fieldsDialogVisible by remember { mutableStateOf(false) }
        var sortDialogVisible by remember { mutableStateOf(false) }
        var experienceDialogVisible by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Исходная таблица
                Button(
                    onClick = {
                        scope.launch {
                            executeQuery(EmployeeQuery())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.TableView,
                        contentDescription = "Все сотрудники"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Все сотрудники")
                }

                // Выбор отображаемых полей
                Button(
                    onClick = {
                        fieldsDialogVisible = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewColumn,
                        contentDescription = "Поля"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Поля")
                }

                // Сортировка
                Button(
                    onClick = {
                        sortDialogVisible = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SortByAlpha,
                        contentDescription = "Сортировка"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Сортировка")
                }

                // Фильтр по стажу
                Button(
                    onClick = {
                        experienceDialogVisible = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Стаж"
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Стаж")
                }
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



package com.example.browser_queries_db

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import kotlinx.serialization.json.JsonArray
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.browser_queries_db.model.EmployeeQuery
import com.example.browser_queries_db.ui.EmployeeTable
import com.example.browser_queries_db.ui.FieldsDialog
import com.example.browser_queries_db.ui.QuickQueryButtons
import com.example.browser_queries_db.ui.SortDialog
import com.example.browser_queries_db.ui.TableControlPanel
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        var employees by remember { mutableStateOf<JsonArray?>(null) }

        var error by remember { mutableStateOf<String?>(null) }

        var fieldsDialogVisible by remember { mutableStateOf(false) }

        var sortDialogVisible by remember { mutableStateOf(false) }


        var currentQuery by remember { mutableStateOf(EmployeeQuery()) }

        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()


        // Локальная функция для вызова запросов к БД
        suspend fun executeQuery(query: EmployeeQuery) {
            currentQuery = query
            employees = null

            try {
                employees = loadEmployees(query)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }

        LaunchedEffect(Unit) {
            try {
                employees = loadEmployees(EmployeeQuery())
            } catch (e: Exception) {
                error = e.message
            }
        }


        // ДИАЛОГИ

        if (sortDialogVisible) {
            SortDialog(
                onDismiss = {
                    sortDialogVisible = false
                },
                onApply = { field, direction ->
                    sortDialogVisible = false

                    scope.launch {
                        executeQuery(
                            currentQuery.copy(
                                sort = field,
                                direction = direction
                            )
                        )
                    }
                }
            )
        }

        if (fieldsDialogVisible) {
            FieldsDialog(
                onDismiss = {
                    fieldsDialogVisible = false
                },
                onApply = { fields ->
                    fieldsDialogVisible = false

                    scope.launch {
                        executeQuery(
                            currentQuery.copy(
                                fields = fields
                            )
                        )
                    }
                }
            )
        }

        if (experienceDialogVisible) {
            ExperienceDialog(
                onDismiss = {
                    experienceDialogVisible = false
                },
                onApply = { experience ->
                    experienceDialogVisible = false

                    scope.launch {
                        executeQuery(
                            currentQuery.copy(
                                experienceMin = experience
                            )
                        )
                    }
                }
            )
        }

        // ОСНОВНОЙ ЭКРАН
        Column(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.background
                )
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuickQueryButtons(
                onQuery = { query ->
                    scope.launch {
                        executeQuery(query)
                    }
                }
            )

            Text(
                text = "Таблица: Сотрудники",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            TableControlPanel(
                onReset = {
                    scope.launch {
                        executeQuery(EmployeeQuery())
                    }
                },
                onFieldsClick = {
                    fieldsDialogVisible = true
                },
                onSortClick = {
                    sortDialogVisible = true
                }
            )

            // Состояние таблицы
            when {
                error != null -> {
                    Text("Error: $error")
                }

                employees == null -> {
                    Text("Loading...")
                }

                else -> {
                    val employeeData = employees!!

                    EmployeeTable(employeeData)
                }
            }
        }
    }
}
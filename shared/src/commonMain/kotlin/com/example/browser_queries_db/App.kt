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
import com.example.browser_queries_db.model.TableQuery
import com.example.browser_queries_db.ui.DatabaseTable
import com.example.browser_queries_db.ui.FieldsDialog
import com.example.browser_queries_db.ui.QuickQueryButtons
import com.example.browser_queries_db.ui.SortDialog
import com.example.browser_queries_db.ui.TableControlPanel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Composable
@Preview
fun App() {
    MaterialTheme {
        var rows by remember { mutableStateOf<JsonArray?>(null) }

        var headers by remember { mutableStateOf<JsonArray?>(null) }

        var error by remember { mutableStateOf<String?>(null) }

        var fieldsDialogVisible by remember { mutableStateOf(false) }

        var sortDialogVisible by remember { mutableStateOf(false) }

        var currentQuery by remember { mutableStateOf(TableQuery()) }

        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()


        // Локальная функция для вызова запросов к БД
        suspend fun executeQuery(query: TableQuery) {
            currentQuery = query
            rows = null

            try {

                rows = loadTableRows(DEFAULT_TABLE_NAME, query)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }

        LaunchedEffect(Unit) {
            try {
                headers = getTableHeaders(DEFAULT_TABLE_NAME)
                rows = loadTableRows(DEFAULT_TABLE_NAME, TableQuery())
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

        if (fieldsDialogVisible && headers != null) {
            val currHeaders = headers!!

            FieldsDialog(
                currHeaders.map { header ->
                    header.jsonObject["column_name"]?.jsonPrimitive?.content.orEmpty()
                },
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
                        executeQuery(TableQuery())
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

                headers == null || rows == null -> {
                    Text("Loading...")
                }

                else -> {
                    val rowData = rows!!
                    val currHeaders = headers!!
                    DatabaseTable(currHeaders, rowData)
                }
            }
        }
    }
}

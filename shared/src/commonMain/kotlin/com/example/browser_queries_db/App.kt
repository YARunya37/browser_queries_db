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
import com.example.browser_queries_db.ui.components.DatabaseTable
import com.example.browser_queries_db.ui.dialogs.FieldsDialog
import com.example.browser_queries_db.ui.components.QuickQueryButtons
import com.example.browser_queries_db.ui.dialogs.SortDialog
import com.example.browser_queries_db.ui.components.TableControlPanel
import com.example.browser_queries_db.ui.components.ToastNotification
import com.example.browser_queries_db.ui.dialogs.CreateTableDialog
import com.example.browser_queries_db.ui.dialogs.EditTableDialog
import io.ktor.client.statement.bodyAsText
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

        var createTableVisible by remember { mutableStateOf(false) }

        var editDialogTables by remember { mutableStateOf<List<String>?>(null) }

        var currentQuery by remember { mutableStateOf(TableQuery()) }

        var isToastVisible by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }

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

        if (sortDialogVisible && headers != null) {
            val currHeaders = headers!!
            SortDialog(
                currHeaders.map { header ->
                    header.jsonObject["column_name"]?.jsonPrimitive?.content.orEmpty()
                },
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

        if (createTableVisible) {
            CreateTableDialog(
                { name, columns ->
                    createTableVisible = false
                    scope.launch {
                        val response = createTable(name, columns)
                        isToastVisible = true
                        toastMessage = response.bodyAsText()
                    }
                },
                onDismiss = { createTableVisible = false }
            )
        }

        if (editDialogTables != null) {
            EditTableDialog(
                tables = editDialogTables!!,
                onDismiss = { editDialogTables = null },
                onApply = { tableName, columns ->
                    editDialogTables = null

                    scope.launch {
                        try {
                            val response = updateTable(tableName, columns)

                            isToastVisible = true
                            toastMessage = response.bodyAsText()

                            // Обновляем отображаемую таблицу, если изменили её структуру
                            if (tableName == DEFAULT_TABLE_NAME) {
                                headers = getTableHeaders(DEFAULT_TABLE_NAME)
                                rows = loadTableRows(DEFAULT_TABLE_NAME, currentQuery)
                            }
                        } catch (e: Exception) {
                            isToastVisible = true
                            toastMessage = e.message ?: "Ошибка обновления таблицы"
                        }
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
            ToastNotification(message = toastMessage, isToastVisible, {
                isToastVisible = false
                toastMessage = ""
            }
            )

            TableControlPanel({
                createTableVisible = true
            }, {
                scope.launch {
                    try {
                        val tables = getTables().mapNotNull { table ->
                            table.jsonObject["table_name"]
                                ?.jsonPrimitive
                                ?.content
                        }

                        editDialogTables = tables
                    } catch (e: Exception) {
                        isToastVisible = true
                        toastMessage = e.message ?: "Ошибка загрузки таблиц"
                    }
                }
            }, {

            }, {

            }
            )

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

                    DatabaseTable(rowData)
                }
            }
        }
    }
}

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
import com.example.browser_queries_db.model.TableRowChanges
import com.example.browser_queries_db.ui.components.ControlContentPanel
import com.example.browser_queries_db.ui.components.DatabaseTable
import com.example.browser_queries_db.ui.components.TableEditor
import com.example.browser_queries_db.ui.dialogs.FieldsDialog
import com.example.browser_queries_db.ui.components.QuickQueryButtons
import com.example.browser_queries_db.ui.dialogs.SortDialog
import com.example.browser_queries_db.ui.components.TableControlPanel
import com.example.browser_queries_db.ui.components.ToastNotification
import com.example.browser_queries_db.ui.dialogs.ChooseTableDialog
import com.example.browser_queries_db.ui.dialogs.CreateTableDialog
import com.example.browser_queries_db.ui.dialogs.EditTableDialog
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
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

        var showDialogTables by remember { mutableStateOf<List<String>?>(null) }

        var deleteDialogTables by remember { mutableStateOf<List<String>?>(null) }

        var currentTableName by remember { mutableStateOf(DEFAULT_TABLE_NAME) }

        var currentQuery by remember { mutableStateOf(TableQuery()) }

        var editMode by remember { mutableStateOf(false) }

        var isToastVisible by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }

        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()


        // Переключает отображаемую таблицу и загружает её структуру и данные
        suspend fun showTable(tableName: String) {
            currentTableName = tableName
            currentQuery = TableQuery()
            editMode = false
            headers = null
            rows = null

            try {
                headers = getTableHeaders(tableName)
                rows = loadTableRows(tableName, TableQuery())
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }

        // Локальная функция для вызова запросов к БД
        suspend fun executeQuery(query: TableQuery) {
            currentQuery = query
            editMode = false
            rows = null

            try {

                rows = loadTableRows(currentTableName, query)
                error = null
            } catch (e: Exception) {
                error = e.message
            }
        }

        // Применяет изменения строк через API: удаление, изменение, добавление.
        // Ошибочная строка не останавливает обработку остальных: ошибки ответа
        // и исключения API собираются и показываются после завершения.
        suspend fun saveRowChanges(
            tableName: String,
            changes: TableRowChanges
        ) {
            val errors = mutableListOf<String>()

            suspend fun performRequest(
                action: String,
                request: suspend () -> HttpResponse
            ) {
                try {
                    val response = request()

                    if (response.status.value !in 200..299) {
                        val detail = response.bodyAsText()
                            .ifBlank { "HTTP ${response.status.value}" }

                        errors += "$action: $detail"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    errors += "$action: ${e.message ?: "Ошибка запроса"}"
                }
            }

            for (id in changes.deletedRowIds) {
                performRequest("Удаление строки $id") {
                    deleteRow(tableName, id)
                }
            }

            for (row in changes.updatedRows) {
                performRequest("Изменение строки ${row.id}") {
                    updateRow(tableName, row.id, row.values)
                }
            }

            for (values in changes.addedRows) {
                performRequest("Добавление строки") {
                    addRow(tableName, values)
                }
            }

            if (errors.isNotEmpty()) {
                throw IllegalStateException(errors.joinToString("\n"))
            }
        }

        LaunchedEffect(Unit) {
            showTable(currentTableName)
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

                            // Показываем отредактированную таблицу
                            showTable(tableName)
                        } catch (e: Exception) {
                            isToastVisible = true
                            toastMessage = e.message ?: "Ошибка обновления таблицы"
                        }
                    }
                }
            )
        }

        if (showDialogTables != null) {
            ChooseTableDialog(
                tables = showDialogTables!!,
                onDismiss = { showDialogTables = null },
                onConfirm = { tableName ->
                    showDialogTables = null

                    scope.launch {
                        showTable(tableName)
                    }
                }
            )
        }

        if (deleteDialogTables != null) {
            ChooseTableDialog(
                tables = deleteDialogTables!!,
                onDismiss = { deleteDialogTables = null },
                onConfirm = { tableName ->
                    deleteDialogTables = null

                    scope.launch {
                        try {
                            val response = deleteTable(tableName)

                            isToastVisible = true
                            toastMessage = if (response.status.value in 200..299) {
                                "Таблица удалена"
                            } else {
                                response.bodyAsText()
                            }

                            if (tableName == currentTableName) {
                                showTable(DEFAULT_TABLE_NAME)
                            }
                        } catch (e: Exception) {
                            isToastVisible = true
                            toastMessage = e.message ?: "Ошибка удаления таблицы"
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
            },
                durationMillis = 10000
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
                scope.launch {
                    try {
                        val tables = getTables().mapNotNull { table ->
                            table.jsonObject["table_name"]
                                ?.jsonPrimitive
                                ?.content
                        }.filter { it != DEFAULT_TABLE_NAME }

                        deleteDialogTables = tables
                    } catch (e: Exception) {
                        isToastVisible = true
                        toastMessage = e.message ?: "Ошибка загрузки таблиц"
                    }
                }
            }, {
                scope.launch {
                    try {
                        val tables = getTables().mapNotNull { table ->
                            table.jsonObject["table_name"]
                                ?.jsonPrimitive
                                ?.content
                        }

                        showDialogTables = tables
                    } catch (e: Exception) {
                        isToastVisible = true
                        toastMessage = e.message ?: "Ошибка загрузки таблиц"
                    }
                }
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
                text = "Таблица: $currentTableName",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            ControlContentPanel(
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
                },
                onEditRowsClick = {
                    editMode = true
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

                    val headerColumns = headers!!.mapNotNull { header ->
                        header.jsonObject["column_name"]
                            ?.jsonPrimitive
                            ?.content
                    }

                    if (editMode) {
                        TableEditor(
                            rows = rowData,
                            columns = headerColumns,
                            onCancel = {
                                editMode = false

                                scope.launch {
                                    executeQuery(currentQuery)
                                }
                            },
                            onSave = { changes ->
                                val tableName = currentTableName
                                val query = currentQuery

                                var toast: String? = null

                                try {
                                    saveRowChanges(tableName, changes)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    toast = e.message ?: "Ошибка сохранения"
                                }

                                try {
                                    rows = loadTableRows(tableName, query)
                                    error = null
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    if (toast == null) {
                                        toast = e.message
                                            ?: "Ошибка загрузки таблицы"
                                    }
                                }

                                isToastVisible = true
                                toastMessage = toast ?: if (changes.isEmpty) {
                                    "Нет изменений"
                                } else {
                                    "Изменения сохранены"
                                }

                                editMode = false
                            }
                        )
                    } else {
                        DatabaseTable(rowData)
                    }
                }
            }
        }
    }
}

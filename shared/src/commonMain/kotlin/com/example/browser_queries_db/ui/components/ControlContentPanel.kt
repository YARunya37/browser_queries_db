package com.example.browser_queries_db.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ControlContentPanel(
    onReset: () -> Unit,
    onFieldsClick: () -> Unit,
    onSortClick: () -> Unit
) {
    Row {
        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.TableView,
                contentDescription = "Все записи"
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Все записи")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onFieldsClick,
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

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSortClick,
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
    }
}
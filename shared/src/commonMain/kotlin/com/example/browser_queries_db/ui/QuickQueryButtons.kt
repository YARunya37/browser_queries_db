package com.example.browser_queries_db.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.browser_queries_db.model.TableQuery

@Composable
fun QuickQueryButtons(
    onQuery: (TableQuery) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Button(
            onClick = {
                onQuery(
                    TableQuery(
                        fields = listOf("fio", "phone", "salary")
                    )
                )
            }
        ) {
            Text("Зарплата сотрудников")
        }

        Button(
            onClick = {
                onQuery(
                    TableQuery(
                        fields = listOf("fio", "address"),
                        sort = "address"
                    )
                )
            }
        ) {
            Text("Отсортировать по адресу")
        }
    }
}

package com.example.browser_queries_db.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@Composable
fun ToastNotification(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    durationMillis: Long = 5000
) {
    // Таймер: автоматически скрывает уведомление по истечении времени
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(durationMillis)
            onDismiss()
        }
    }

    if (isVisible) {
        Popup(
            alignment = Alignment.BottomEnd,
            properties = PopupProperties(
                focusable = false, // Не перехватывает клики, экран под ним остается рабочим
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            // Анимация плавного появления и исчезновения
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer, // Полупрозрачный черный фон
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLargeEmphasized
                )

            }
        }
    }
}

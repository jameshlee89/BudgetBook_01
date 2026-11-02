package com.budgetbook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary = Color(0xFF2E7D6B)

@Composable
fun BudgetBookTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF7FD3BE))
    } else {
        lightColorScheme(primary = Primary)
    }
    MaterialTheme(colorScheme = colors, content = content)
}

/** 지출 금액 표시 색 */
val ExpenseColor = Color(0xFFD32F2F)

/** 취소(환불) 금액 표시 색 */
val RefundColor = Color(0xFF1976D2)

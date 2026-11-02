package com.budgetbook.app.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetbook.app.domain.MoneyFormat
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionSource
import com.budgetbook.app.domain.model.TransactionType
import com.budgetbook.app.ui.components.MonthSelector
import com.budgetbook.app.ui.theme.ExpenseColor
import com.budgetbook.app.ui.theme.RefundColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val ROW_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm")

/** [REQ-LIST-001] 월별 내역 리스트(최신순). 항목을 누르면 수정 화면을 연다. [REQ-LIST-002] */
@Composable
fun TransactionListScreen(
    month: YearMonth,
    transactions: List<Transaction>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onImportInbox: () -> Unit,
    onClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        item { MonthSelector(month, onPreviousMonth, onNextMonth, Modifier.padding(horizontal = 16.dp)) }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${transactions.size}건 · 합계 ${MoneyFormat.won(transactions.sumOf { it.signedAmount })}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                // [REQ-SMS-005]
                TextButton(onClick = onImportInbox) { Text("문자함에서 가져오기") }
            }
            HorizontalDivider()
        }
        if (transactions.isEmpty()) {
            item {
                Text(
                    "내역이 없습니다. 카드 승인 문자가 오면 자동으로 기입됩니다.",
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(transactions, key = { it.id }) { tx ->
            TransactionRow(tx, onClick = { onClick(tx) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(Color(tx.category.colorArgb), CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val details = listOfNotNull(
                tx.dateTime.format(ROW_DATE_FORMAT),
                tx.category.label,
                tx.cardName,
                if (tx.installmentMonths > 0) "${tx.installmentMonths}개월" else null,
                if (tx.source == TransactionSource.MANUAL) "수동" else null,
                if (tx.manuallyEdited) "수정됨" else null,
            )
            Text(
                details.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tx.memo.isNotBlank()) {
                Text(tx.memo, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        val isCancel = tx.type == TransactionType.CANCEL
        Text(
            text = if (isCancel) "취소 +${MoneyFormat.won(tx.amount)}" else "-${MoneyFormat.won(tx.amount)}",
            color = if (isCancel) RefundColor else ExpenseColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

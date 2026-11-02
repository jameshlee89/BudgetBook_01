package com.budgetbook.app.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetbook.app.domain.TransactionInput
import com.budgetbook.app.domain.TransactionValidator
import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionType
import com.budgetbook.app.ui.theme.ExpenseColor
import java.time.LocalDateTime

/**
 * 내역 추가/수정 다이얼로그.
 * [REQ-LIST-002] 금액·사용처·항목·일시·메모·구분 수정
 * [REQ-LIST-003] 삭제(확인 후)
 * [REQ-LIST-004] original == null 이면 새 내역 추가
 * [REQ-LIST-005] 저장 시 검증 오류를 보여준다.
 *
 * @param onSave 검증 오류 목록을 돌려준다. 비어 있으면 저장된 것으로 보고 닫는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditDialog(
    original: Transaction?,
    onDismiss: () -> Unit,
    onSave: (TransactionInput) -> List<String>,
    onDelete: (Transaction) -> Unit,
) {
    var amount by remember { mutableStateOf(original?.amount?.toString().orEmpty()) }
    var merchant by remember { mutableStateOf(original?.merchant.orEmpty()) }
    var category by remember { mutableStateOf(original?.category ?: Category.ETC) }
    var dateTime by remember {
        mutableStateOf((original?.dateTime ?: LocalDateTime.now()).format(TransactionValidator.DATE_TIME_FORMAT))
    }
    var memo by remember { mutableStateOf(original?.memo.orEmpty()) }
    var type by remember { mutableStateOf(original?.type ?: TransactionType.EXPENSE) }
    var errors by remember { mutableStateOf(emptyList<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original == null) "내역 추가" else "내역 수정") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE }, label = { Text("지출") })
                    FilterChip(selected = type == TransactionType.CANCEL, onClick = { type = TransactionType.CANCEL }, label = { Text("취소/환불") })
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("금액(원)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("사용처") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("항목", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(Category.entries) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                OutlinedTextField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = { Text("일시 (yyyy-MM-dd HH:mm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth(),
                )
                original?.cardName?.let {
                    Text("카드: $it", style = MaterialTheme.typography.bodySmall)
                }
                errors.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (original != null) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.padding(top = 4.dp)) {
                        Text("이 내역 삭제", color = ExpenseColor)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                errors = onSave(TransactionInput(amount, merchant, category, dateTime, memo, type))
                if (errors.isEmpty()) onDismiss()
            }) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("삭제할까요?") },
            text = { Text("공유된 구성원 모두의 가계부에서 삭제됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(original)
                    confirmDelete = false
                    onDismiss()
                }) { Text("삭제", color = ExpenseColor) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

package com.budgetbook.app.ui.share

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.budgetbook.app.domain.model.Ledger
import com.budgetbook.app.domain.model.UserInfo

/**
 * 공유 설정 화면.
 * [REQ-SHARE-002] 로그아웃
 * [REQ-SHARE-003] 새 가계부 만들기
 * [REQ-SHARE-004] 초대 코드 표시/공유
 * [REQ-SHARE-005] 초대 코드로 참여
 */
@Composable
fun ShareScreen(
    user: UserInfo,
    ledger: Ledger?,
    onCreateLedger: (String) -> Unit,
    onJoinLedger: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var newLedgerName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (ledger != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ledger.name, style = MaterialTheme.typography.titleLarge)
                    Text("초대 코드", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ledger.inviteCode,
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "'${ledger.name}' 가계부에 초대합니다. 앱의 공유 탭에서 초대 코드 ${ledger.inviteCode} 를 입력하세요.",
                                )
                            }
                            context.startActivity(Intent.createChooser(send, "초대 코드 보내기"))
                        }) { Text("보내기") }
                    }
                    HorizontalDivider()
                    Text("구성원 ${ledger.members.size}명", style = MaterialTheme.typography.labelMedium)
                    ledger.members.forEach { uid ->
                        val name = ledger.memberNames[uid] ?: uid.take(6)
                        val suffix = if (uid == ledger.ownerUid) " (만든 사람)" else ""
                        Text("• $name$suffix")
                    }
                }
            }
        }

        Text("초대 코드로 참여", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it },
                label = { Text("6자리 코드") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onJoinLedger(joinCode); joinCode = "" }, enabled = joinCode.isNotBlank()) { Text("참여") }
        }

        Text("새 가계부 만들기", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newLedgerName,
                onValueChange = { newLedgerName = it },
                label = { Text("가계부 이름") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { onCreateLedger(newLedgerName); newLedgerName = "" }, enabled = newLedgerName.isNotBlank()) {
                Text("만들기")
            }
        }

        HorizontalDivider()
        Text("${user.displayName} (${user.email})", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onSignOut) { Text("로그아웃") }
    }
}

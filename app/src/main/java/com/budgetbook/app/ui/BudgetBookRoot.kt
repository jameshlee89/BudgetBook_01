package com.budgetbook.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetbook.app.domain.model.Ledger
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.ui.chart.StatsScreen
import com.budgetbook.app.ui.list.TransactionEditDialog
import com.budgetbook.app.ui.list.TransactionListScreen
import com.budgetbook.app.ui.share.ShareScreen

private enum class Tab(val label: String) { STATS("통계"), LIST("내역"), SHARE("공유") }

private val SMS_PERMISSIONS = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)

@Composable
fun BudgetBookRoot(viewModel: MainViewModel, activity: Activity) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val current = user
    if (current == null) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            LoginScreen(onSignIn = { viewModel.signIn(activity) }, modifier = Modifier.padding(padding))
        }
    } else {
        MainScaffold(viewModel, snackbar)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(viewModel: MainViewModel, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsStateWithLifecycle()
    val ledgers by viewModel.ledgers.collectAsStateWithLifecycle()
    val ledger by viewModel.currentLedger.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.STATS) }
    // null = 닫힘, EditTarget(null) = 새 내역, EditTarget(tx) = 수정
    var editing by remember { mutableStateOf<EditTarget?>(null) }

    // [REQ-SMS-008] 문자 권한 요청. 거부하면 배너로 다시 요청할 수 있게 한다.
    var smsGranted by remember { mutableStateOf(hasSmsPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        smsGranted = result.values.all { it }
    }
    LaunchedEffect(Unit) { if (!smsGranted) permissionLauncher.launch(SMS_PERMISSIONS) }

    Scaffold(
        topBar = {
            TopAppBar(title = { LedgerSwitcher(ledger, ledgers, onSelect = viewModel::selectLedger) })
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.STATS -> Icons.Default.Home
                                    Tab.LIST -> Icons.Default.List
                                    Tab.SHARE -> Icons.Default.Person
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.LIST) {
                // [REQ-LIST-004]
                FloatingActionButton(onClick = { editing = EditTarget(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "내역 추가")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!smsGranted) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "문자 권한이 없어 카드 승인 내역을 자동 기입할 수 없습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { permissionLauncher.launch(SMS_PERMISSIONS) }) { Text("허용") }
                    }
                }
            }
            when (tab) {
                Tab.STATS -> StatsScreen(month, summary, viewModel::previousMonth, viewModel::nextMonth)
                Tab.LIST -> TransactionListScreen(
                    month = month,
                    transactions = transactions,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    onImportInbox = {
                        if (smsGranted) viewModel.importFromInbox() else permissionLauncher.launch(SMS_PERMISSIONS)
                    },
                    onClick = { editing = EditTarget(it) },
                )
                Tab.SHARE -> user?.let { u ->
                    ShareScreen(
                        user = u,
                        ledger = ledger,
                        onCreateLedger = viewModel::createLedger,
                        onJoinLedger = viewModel::joinLedger,
                        onSignOut = { viewModel.signOut(context) },
                    )
                }
            }
        }
    }

    editing?.let { target ->
        TransactionEditDialog(
            original = target.transaction,
            onDismiss = { editing = null },
            onSave = { input -> viewModel.saveTransaction(target.transaction, input) },
            onDelete = viewModel::deleteTransaction,
        )
    }
}

private class EditTarget(val transaction: Transaction?)

/** [REQ-SHARE-008] 현재 가계부 선택 */
@Composable
private fun LedgerSwitcher(current: Ledger?, ledgers: List<Ledger>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.clickable(enabled = ledgers.size > 1) { expanded = true }, verticalAlignment = Alignment.CenterVertically) {
        Text(current?.name ?: "가계부 불러오는 중…")
        if (ledgers.size > 1) Icon(Icons.Default.ArrowDropDown, contentDescription = "가계부 선택")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ledgers.forEach { l ->
            DropdownMenuItem(
                text = { Text(if (l.members.size > 1) "${l.name} (${l.members.size}명)" else l.name) },
                onClick = {
                    onSelect(l.id)
                    expanded = false
                },
            )
        }
    }
}

private fun hasSmsPermission(context: android.content.Context): Boolean = SMS_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

package com.budgetbook.app.ui

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.budgetbook.app.AppContainer
import com.budgetbook.app.data.AuthRepository
import com.budgetbook.app.data.LedgerRepository
import com.budgetbook.app.data.SettingsStore
import com.budgetbook.app.domain.MonthlySummary
import com.budgetbook.app.domain.MonthlySummaryCalculator
import com.budgetbook.app.domain.TransactionIdGenerator
import com.budgetbook.app.domain.TransactionInput
import com.budgetbook.app.domain.TransactionValidator
import com.budgetbook.app.domain.ValidationResult
import com.budgetbook.app.domain.model.Ledger
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionSource
import com.budgetbook.app.domain.model.UserInfo
import com.budgetbook.app.sms.SmsInboxImporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val auth: AuthRepository,
    private val repository: LedgerRepository,
    private val settings: SettingsStore,
    private val inboxImporter: SmsInboxImporter,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val user: StateFlow<UserInfo?> = auth.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUserNow())

    val ledgers: StateFlow<List<Ledger>> = user
        .map { it?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeLedgers(uid) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val selectedLedgerId = MutableStateFlow(settings.currentLedgerId)

    /** [REQ-SHARE-008] 선택한 가계부가 없거나 목록에서 사라지면 첫 번째 가계부를 쓴다. */
    val currentLedger: StateFlow<Ledger?> = combine(ledgers, selectedLedgerId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    /** [REQ-LIST-001] [REQ-SHARE-006] 현재 가계부·월의 거래를 실시간 구독 */
    val transactions: StateFlow<List<Transaction>> = combine(
        currentLedger.map { it?.id }.distinctUntilChanged(),
        _month,
    ) { id, m -> id to m }
        .flatMapLatest { (id, m) -> if (id == null) flowOf(emptyList()) else repository.observeTransactions(id, m) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** [REQ-CHART-001] */
    val summary: StateFlow<MonthlySummary> = combine(transactions, _month) { list, m ->
        MonthlySummaryCalculator.calculate(m, list)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MonthlySummary.empty(YearMonth.now()))

    init {
        // [REQ-SHARE-003] 최초 로그인 시 기본 가계부 생성
        viewModelScope.launch {
            user.filterNotNull().distinctUntilChanged { a, b -> a.uid == b.uid }.collect { u ->
                runCatching { repository.ensureDefaultLedger(u) }
                    .onFailure { _messages.emit("기본 가계부 생성 실패: ${it.message}") }
            }
        }
        // 문자 수신기가 같은 가계부에 기입하도록 저장해 둔다.
        viewModelScope.launch {
            currentLedger.filterNotNull().map { it.id }.distinctUntilChanged().collect { settings.currentLedgerId = it }
        }
    }

    // region 인증 [REQ-SHARE-001] [REQ-SHARE-002]
    fun signIn(activity: Activity) = viewModelScope.launch {
        runCatching { auth.signIn(activity) }.onFailure { _messages.emit("로그인 실패: ${it.message}") }
    }

    fun signOut(context: Context) = viewModelScope.launch {
        settings.currentLedgerId = null
        selectedLedgerId.value = null
        runCatching { auth.signOut(context) }
    }
    // endregion

    // region 월 이동 [REQ-CHART-002]
    fun previousMonth() = _month.update { it.minusMonths(1) }
    fun nextMonth() = _month.update { it.plusMonths(1) }
    // endregion

    // region 가계부/공유 [REQ-SHARE-003] [REQ-SHARE-005] [REQ-SHARE-008]
    fun selectLedger(ledgerId: String) {
        selectedLedgerId.value = ledgerId
    }

    fun createLedger(name: String) = viewModelScope.launch {
        val u = user.value ?: return@launch
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _messages.emit("가계부 이름을 입력하세요.")
            return@launch
        }
        runCatching { repository.createLedger(trimmed, u) }
            .onSuccess {
                selectLedger(it.id)
                _messages.emit("'${it.name}' 가계부를 만들었습니다.")
            }
            .onFailure { _messages.emit("가계부 생성 실패: ${it.message}") }
    }

    fun joinLedger(code: String) = viewModelScope.launch {
        val u = user.value ?: return@launch
        repository.joinLedger(code, u)
            .onSuccess {
                selectLedger(it)
                _messages.emit("가계부에 참여했습니다.")
            }
            .onFailure { _messages.emit("참여 실패: ${it.message}") }
    }
    // endregion

    // region 내역 수정 [REQ-LIST-002] [REQ-LIST-003] [REQ-LIST-004] [REQ-LIST-005]
    /**
     * @param original 수정할 거래. null 이면 새 거래를 추가한다.
     * @return 검증 오류 목록. 비어 있으면 저장을 시작한 것이다.
     */
    fun saveTransaction(original: Transaction?, input: TransactionInput): List<String> {
        val ledger = currentLedger.value ?: return listOf("가계부가 없습니다.")
        val valid = when (val result = TransactionValidator.validate(input)) {
            is ValidationResult.Invalid -> return result.errors
            is ValidationResult.Valid -> result
        }
        val transaction = original?.copy(
            amount = valid.amount,
            merchant = valid.merchant,
            category = input.category,
            dateTime = valid.dateTime,
            type = input.type,
            memo = input.memo.trim(),
            manuallyEdited = true,
        ) ?: Transaction(
            id = TransactionIdGenerator.newManualId(),
            ledgerId = ledger.id,
            amount = valid.amount,
            merchant = valid.merchant,
            category = input.category,
            dateTime = valid.dateTime,
            type = input.type,
            memo = input.memo.trim(),
            source = TransactionSource.MANUAL,
            createdBy = user.value?.uid.orEmpty(),
        )
        viewModelScope.launch {
            runCatching { repository.upsertTransaction(transaction) }
                .onFailure { _messages.emit("저장 실패: ${it.message}") }
        }
        return emptyList()
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        runCatching { repository.deleteTransaction(transaction.ledgerId, transaction.id) }
            .onFailure { _messages.emit("삭제 실패: ${it.message}") }
    }
    // endregion

    /** [REQ-SMS-005] */
    fun importFromInbox() = viewModelScope.launch {
        runCatching { inboxImporter.import(_month.value) }
            .onSuccess { _messages.emit("문자 ${it.scanned}건 확인 · ${it.added}건 추가 · 중복 ${it.duplicates}건") }
            .onFailure { _messages.emit("문자함을 읽지 못했습니다: ${it.message}") }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(
                    auth = container.authRepository,
                    repository = container.ledgerRepository,
                    settings = container.settings,
                    inboxImporter = container.inboxImporter,
                )
            }
        }
    }
}

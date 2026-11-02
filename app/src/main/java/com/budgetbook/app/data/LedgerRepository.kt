package com.budgetbook.app.data

import com.budgetbook.app.domain.model.Ledger
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.UserInfo
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface LedgerRepository {
    /** 사용자가 구성원인 가계부 목록(실시간). [REQ-SHARE-006] */
    fun observeLedgers(uid: String): Flow<List<Ledger>>

    /** 서버에 가계부가 하나도 없으면 기본 가계부를 만든다. [REQ-SHARE-003] */
    suspend fun ensureDefaultLedger(user: UserInfo)

    suspend fun createLedger(name: String, owner: UserInfo): Ledger

    /** 초대 코드로 가계부에 참여한다. [REQ-SHARE-005] */
    suspend fun joinLedger(code: String, user: UserInfo): Result<String>

    /** 해당 월 거래(실시간, 최신순, 삭제 제외). [REQ-LIST-001] */
    fun observeTransactions(ledgerId: String, month: YearMonth): Flow<List<Transaction>>

    /** [REQ-LIST-002] [REQ-LIST-004] */
    suspend fun upsertTransaction(transaction: Transaction)

    /**
     * 같은 ID 의 문서가 없을 때만 저장한다. 삭제(soft delete)된 문서도 "있음"으로 본다.
     * @return 새로 저장했으면 true [REQ-SMS-006]
     */
    suspend fun addIfAbsent(transaction: Transaction): Boolean

    /** [REQ-LIST-003] */
    suspend fun deleteTransaction(ledgerId: String, transactionId: String)
}

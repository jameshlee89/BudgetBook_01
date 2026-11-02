package com.budgetbook.app.domain

import com.budgetbook.app.data.LedgerRepository
import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.Ledger
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionSource
import com.budgetbook.app.domain.model.TransactionType
import com.budgetbook.app.domain.model.UserInfo
import com.budgetbook.app.domain.sms.CardSmsParser
import com.budgetbook.app.sms.IngestResult
import com.budgetbook.app.sms.SmsIngestor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

/** REQ-SMS-001, REQ-SMS-006, REQ-SMS-007, REQ-SHARE-008 */
class SmsIngestorTest {

    /** Firestore 저장소의 addIfAbsent/soft delete 동작을 흉내 낸다. */
    private class FakeRepository : LedgerRepository {
        val docs = linkedMapOf<String, Transaction>()
        val deleted = mutableSetOf<String>()

        override fun observeLedgers(uid: String): Flow<List<Ledger>> = emptyFlow()
        override suspend fun ensureDefaultLedger(user: UserInfo) = Unit
        override suspend fun createLedger(name: String, owner: UserInfo): Ledger = error("unused")
        override suspend fun joinLedger(code: String, user: UserInfo): Result<String> = error("unused")
        override fun observeTransactions(ledgerId: String, month: YearMonth): Flow<List<Transaction>> = emptyFlow()
        override suspend fun upsertTransaction(transaction: Transaction) {
            docs[transaction.id] = transaction
        }
        override suspend fun addIfAbsent(transaction: Transaction): Boolean {
            if (docs.containsKey(transaction.id)) return false
            docs[transaction.id] = transaction
            return true
        }
        override suspend fun deleteTransaction(ledgerId: String, transactionId: String) {
            deleted += transactionId
        }
    }

    private val sms = "[Web발신]\n신한카드(1234)승인 홍*동 12,500원(일시불)11/02 12:34 스타벅스 강남점 누적1,234,567원"
    private val receivedAt = LocalDateTime.of(2026, 11, 2, 12, 35)

    private fun ingestor(repo: LedgerRepository, ledgerId: String? = "ledger-A", uid: String? = "user-1") =
        SmsIngestor(repo, { ledgerId }, { uid })

    @Test
    fun `TC-SMS-001-01 카드 문자는 현재 가계부에 자동 기입`() = runTest {
        val repo = FakeRepository()
        val result = ingestor(repo).ingest(sms, receivedAt)

        assertEquals(IngestResult.ADDED, result)
        val tx = repo.docs.values.single()
        assertEquals("ledger-A", tx.ledgerId)
        assertEquals(12_500L, tx.amount)
        assertEquals("스타벅스 강남점", tx.merchant)
        assertEquals(Category.CAFE, tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionSource.SMS, tx.source)
        assertEquals("user-1", tx.createdBy)
    }

    @Test
    fun `TC-SMS-001-03 로그인 또는 가계부가 없으면 기입하지 않음`() = runTest {
        val repo = FakeRepository()
        assertEquals(IngestResult.NOT_SIGNED_IN, ingestor(repo, uid = null).ingest(sms, receivedAt))
        assertEquals(IngestResult.NO_LEDGER, ingestor(repo, ledgerId = null).ingest(sms, receivedAt))
        assertEquals(IngestResult.NOT_CARD_SMS, ingestor(repo).ingest("안녕하세요", receivedAt))
        assertTrue(repo.docs.isEmpty())
    }

    @Test
    fun `TC-SMS-006-01 같은 문자는 같은 ID`() {
        val a = CardSmsParser.parse(sms, receivedAt)!!
        // 문자함 가져오기는 수신 시각이 조금 달라도 같은 ID 가 되어야 한다.
        val b = CardSmsParser.parse(sms, receivedAt.plusMinutes(3))!!
        assertEquals(TransactionIdGenerator.fromSms(a), TransactionIdGenerator.fromSms(b))
    }

    @Test
    fun `TC-SMS-006-02 내용이 다르면 다른 ID`() {
        val a = CardSmsParser.parse(sms, receivedAt)!!
        val other = TransactionIdGenerator.fromSms(a.copy(amount = 12_600))
        val cancel = TransactionIdGenerator.fromSms(a.copy(isCancel = true))
        assertNotEquals(TransactionIdGenerator.fromSms(a), other)
        assertNotEquals(TransactionIdGenerator.fromSms(a), cancel)
        assertNotEquals(TransactionIdGenerator.newManualId(), TransactionIdGenerator.newManualId())
    }

    @Test
    fun `TC-SMS-006-03 같은 문자를 두 번 받아도 한 건만 기입되고 수정 내용 유지`() = runTest {
        val repo = FakeRepository()
        val ingestor = ingestor(repo)
        assertEquals(IngestResult.ADDED, ingestor.ingest(sms, receivedAt))

        val edited = repo.docs.values.single().copy(category = Category.FOOD, manuallyEdited = true)
        repo.upsertTransaction(edited)

        assertEquals(IngestResult.DUPLICATE, ingestor.ingest(sms, receivedAt.plusMinutes(1)))
        assertEquals(1, repo.docs.size)
        assertEquals(Category.FOOD, repo.docs.values.single().category)
    }

    @Test
    fun `TC-SHARE-008-02 선택된 가계부가 바뀌면 이후 문자는 새 가계부에 기입`() = runTest {
        val repo = FakeRepository()
        var current = "ledger-A"
        val ingestor = SmsIngestor(repo, { current }, { "user-1" })

        ingestor.ingest(sms, receivedAt)
        current = "ledger-B"
        ingestor.ingest("삼성카드 승인 홍*동\n45,000원 일시불\n11/01 19:05 교촌치킨", receivedAt)

        assertEquals(listOf("ledger-A", "ledger-B"), repo.docs.values.map { it.ledgerId })
    }
}

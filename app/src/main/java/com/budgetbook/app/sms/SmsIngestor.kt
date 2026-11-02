package com.budgetbook.app.sms

import com.budgetbook.app.data.LedgerRepository
import com.budgetbook.app.domain.CategoryClassifier
import com.budgetbook.app.domain.TransactionIdGenerator
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionSource
import com.budgetbook.app.domain.model.TransactionType
import com.budgetbook.app.domain.sms.CardSmsParser
import java.time.LocalDateTime

enum class IngestResult { ADDED, DUPLICATE, NOT_CARD_SMS, NOT_SIGNED_IN, NO_LEDGER }

/**
 * 문자 한 건을 해석해 현재 가계부에 기입한다. 실시간 수신과 문자함 가져오기가 공통으로 쓴다.
 *
 * [REQ-SMS-001] 카드 문자 → 현재 가계부에 자동 기입
 * [REQ-SMS-006] 결정적 ID + addIfAbsent 로 중복 방지
 * [REQ-SMS-007] 가맹점으로 항목 자동 분류
 * [REQ-SHARE-008] 기입 대상은 사용자가 선택한 현재 가계부
 */
class SmsIngestor(
    private val repository: LedgerRepository,
    private val currentLedgerId: () -> String?,
    private val currentUserId: () -> String?,
) {
    suspend fun ingest(body: String, receivedAt: LocalDateTime): IngestResult {
        val parsed = CardSmsParser.parse(body, receivedAt) ?: return IngestResult.NOT_CARD_SMS
        val uid = currentUserId() ?: return IngestResult.NOT_SIGNED_IN
        val ledgerId = currentLedgerId() ?: return IngestResult.NO_LEDGER

        val transaction = Transaction(
            id = TransactionIdGenerator.fromSms(parsed),
            ledgerId = ledgerId,
            amount = parsed.amount,
            merchant = parsed.merchant,
            category = CategoryClassifier.classify(parsed.merchant),
            dateTime = parsed.dateTime,
            type = if (parsed.isCancel) TransactionType.CANCEL else TransactionType.EXPENSE,
            cardName = parsed.cardName,
            installmentMonths = parsed.installmentMonths,
            source = TransactionSource.SMS,
            createdBy = uid,
        )
        return if (repository.addIfAbsent(transaction)) IngestResult.ADDED else IngestResult.DUPLICATE
    }
}

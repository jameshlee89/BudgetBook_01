package com.budgetbook.app.domain.model

import java.time.LocalDateTime

enum class TransactionType {
    /** 카드 승인(지출) */
    EXPENSE,

    /** 승인 취소(환불). 월 합계에서 차감된다. [REQ-SMS-003] */
    CANCEL,
}

enum class TransactionSource { SMS, MANUAL }

data class Transaction(
    val id: String,
    val ledgerId: String,
    val amount: Long,
    val merchant: String,
    val category: Category,
    val dateTime: LocalDateTime,
    val type: TransactionType = TransactionType.EXPENSE,
    val cardName: String? = null,
    val installmentMonths: Int = 0,
    val memo: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val createdBy: String = "",
    val manuallyEdited: Boolean = false,
) {
    /** 지출은 +, 취소는 - 로 합산한다. */
    val signedAmount: Long get() = if (type == TransactionType.CANCEL) -amount else amount
}

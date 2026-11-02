package com.budgetbook.app.domain

import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class TransactionInput(
    val amountText: String,
    val merchant: String,
    val category: Category,
    val dateTimeText: String,
    val memo: String,
    val type: TransactionType,
)

sealed interface ValidationResult {
    data class Valid(val amount: Long, val merchant: String, val dateTime: LocalDateTime) : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

/** 수동 추가/수정 입력값 검증. [REQ-LIST-005] */
object TransactionValidator {

    val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    const val MAX_AMOUNT = 1_000_000_000L
    const val MAX_MERCHANT_LENGTH = 50
    const val MAX_MEMO_LENGTH = 200

    fun validate(input: TransactionInput): ValidationResult {
        val errors = mutableListOf<String>()

        val amount = input.amountText.replace(",", "").trim().toLongOrNull()
        when {
            amount == null -> errors += "금액은 숫자로 입력하세요."
            amount <= 0 -> errors += "금액은 0보다 커야 합니다."
            amount > MAX_AMOUNT -> errors += "금액이 너무 큽니다."
        }

        val merchant = input.merchant.trim()
        when {
            merchant.isEmpty() -> errors += "사용처를 입력하세요."
            merchant.length > MAX_MERCHANT_LENGTH -> errors += "사용처는 ${MAX_MERCHANT_LENGTH}자 이하로 입력하세요."
        }

        if (input.memo.length > MAX_MEMO_LENGTH) errors += "메모는 ${MAX_MEMO_LENGTH}자 이하로 입력하세요."

        val dateTime = try {
            LocalDateTime.parse(input.dateTimeText.trim(), DATE_TIME_FORMAT)
        } catch (e: DateTimeParseException) {
            errors += "일시는 yyyy-MM-dd HH:mm 형식으로 입력하세요."
            null
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid(amount!!, merchant, dateTime!!)
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}

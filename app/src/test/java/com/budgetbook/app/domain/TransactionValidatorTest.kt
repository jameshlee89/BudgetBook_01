package com.budgetbook.app.domain

import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** REQ-LIST-005 */
class TransactionValidatorTest {

    private fun input(
        amount: String = "12,500",
        merchant: String = "스타벅스",
        dateTime: String = "2026-11-02 12:34",
        memo: String = "",
    ) = TransactionInput(amount, merchant, Category.CAFE, dateTime, memo, TransactionType.EXPENSE)

    private fun errorsOf(i: TransactionInput) =
        (TransactionValidator.validate(i) as? ValidationResult.Invalid)?.errors.orEmpty()

    @Test
    fun `TC-LIST-005-01 올바른 입력은 통과하고 쉼표와 공백을 정리`() {
        val result = TransactionValidator.validate(input(merchant = "  스타벅스  "))
        assertTrue(result is ValidationResult.Valid)
        result as ValidationResult.Valid
        assertEquals(12_500L, result.amount)
        assertEquals("스타벅스", result.merchant)
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 34), result.dateTime)
    }

    @Test
    fun `TC-LIST-005-02 금액 오류`() {
        assertEquals(1, errorsOf(input(amount = "abc")).size)
        assertEquals(1, errorsOf(input(amount = "0")).size)
        assertEquals(1, errorsOf(input(amount = "-100")).size)
        assertEquals(1, errorsOf(input(amount = "1000000001")).size)
        assertTrue(errorsOf(input(amount = "1000000000")).isEmpty())
    }

    @Test
    fun `TC-LIST-005-03 사용처와 메모 길이 오류`() {
        assertEquals(1, errorsOf(input(merchant = "   ")).size)
        assertEquals(1, errorsOf(input(merchant = "가".repeat(51))).size)
        assertEquals(1, errorsOf(input(memo = "a".repeat(201))).size)
    }

    @Test
    fun `TC-LIST-005-04 일시 형식 오류`() {
        assertEquals(1, errorsOf(input(dateTime = "2026/11/02 12:34")).size)
        assertEquals(1, errorsOf(input(dateTime = "2026-13-02 12:34")).size)
        assertEquals(1, errorsOf(input(dateTime = "")).size)
    }

    @Test
    fun `TC-LIST-005-05 여러 오류를 한 번에 보고`() {
        assertEquals(3, errorsOf(input(amount = "", merchant = "", dateTime = "x")).size)
    }
}

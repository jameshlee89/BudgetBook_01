package com.budgetbook.app.domain

import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth

/** REQ-CHART-001, REQ-CHART-003, REQ-SMS-003 */
class MonthlySummaryCalculatorTest {

    private val november = YearMonth.of(2026, 11)

    private fun tx(
        amount: Long,
        category: Category,
        day: LocalDateTime = LocalDateTime.of(2026, 11, 5, 12, 0),
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        id = "$amount-$category-$day-$type",
        ledgerId = "L",
        amount = amount,
        merchant = "m",
        category = category,
        dateTime = day,
        type = type,
    )

    @Test
    fun `TC-CHART-001-01 항목별 합계`() {
        val summary = MonthlySummaryCalculator.calculate(
            november,
            listOf(tx(10_000, Category.FOOD), tx(5_000, Category.FOOD), tx(3_000, Category.CAFE)),
        )
        assertEquals(18_000L, summary.total)
        assertEquals(15_000L, summary.slices.first { it.category == Category.FOOD }.amount)
        assertEquals(3_000L, summary.slices.first { it.category == Category.CAFE }.amount)
    }

    @Test
    fun `TC-CHART-001-03 다른 달 거래는 제외`() {
        val summary = MonthlySummaryCalculator.calculate(
            november,
            listOf(
                tx(10_000, Category.FOOD),
                tx(99_000, Category.FOOD, LocalDateTime.of(2026, 10, 31, 23, 59)),
                tx(77_000, Category.FOOD, LocalDateTime.of(2026, 12, 1, 0, 0)),
            ),
        )
        assertEquals(10_000L, summary.total)
    }

    @Test
    fun `TC-CHART-001-04 내역이 없으면 빈 요약`() {
        val summary = MonthlySummaryCalculator.calculate(november, emptyList())
        assertEquals(0L, summary.total)
        assertTrue(summary.slices.isEmpty())
    }

    @Test
    fun `TC-CHART-003-01 비율 합은 1이고 금액 내림차순`() {
        val summary = MonthlySummaryCalculator.calculate(
            november,
            listOf(tx(1_000, Category.CAFE), tx(6_000, Category.FOOD), tx(3_000, Category.TRANSPORT)),
        )
        assertEquals(listOf(Category.FOOD, Category.TRANSPORT, Category.CAFE), summary.slices.map { it.category })
        assertEquals(1.0f, summary.slices.sumOf { it.ratio.toDouble() }.toFloat(), 0.0001f)
        assertEquals(0.6f, summary.slices[0].ratio, 0.0001f)
    }

    @Test
    fun `TC-CHART-003-02 금액 표기`() {
        assertEquals("12,500원", MoneyFormat.won(12_500))
        assertEquals("0원", MoneyFormat.won(0))
        assertEquals("1,234,567원", MoneyFormat.won(1_234_567))
    }

    @Test
    fun `TC-SMS-003-02 취소 거래는 항목 합계에서 차감`() {
        val summary = MonthlySummaryCalculator.calculate(
            november,
            listOf(
                tx(10_000, Category.FOOD),
                tx(4_000, Category.FOOD, type = TransactionType.CANCEL),
                tx(3_000, Category.CAFE),
                tx(3_000, Category.CAFE, type = TransactionType.CANCEL),
            ),
        )
        assertEquals(6_000L, summary.total)
        // 순지출이 0 이 된 항목은 그래프에서 빠진다.
        assertEquals(listOf(Category.FOOD), summary.slices.map { it.category })
    }
}

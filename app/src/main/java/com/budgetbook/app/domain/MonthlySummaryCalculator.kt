package com.budgetbook.app.domain

import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.Transaction
import java.time.YearMonth

data class CategorySlice(
    val category: Category,
    val amount: Long,
    /** 0.0 ~ 1.0 */
    val ratio: Float,
)

data class MonthlySummary(
    val month: YearMonth,
    val total: Long,
    val slices: List<CategorySlice>,
) {
    companion object {
        fun empty(month: YearMonth) = MonthlySummary(month, 0, emptyList())
    }
}

/**
 * 월별 항목별 지출 합계를 계산한다.
 *
 * [REQ-CHART-001] 해당 월 거래만 항목별로 합산한다.
 * [REQ-CHART-003] 금액 내림차순 조각과 비율, 총액을 제공한다.
 * [REQ-SMS-003] 취소 거래는 해당 항목 합계에서 차감한다.
 */
object MonthlySummaryCalculator {

    fun calculate(month: YearMonth, transactions: List<Transaction>): MonthlySummary {
        val byCategory = transactions
            .filter { YearMonth.from(it.dateTime) == month }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.signedAmount } }
            .filterValues { it > 0 }

        val total = byCategory.values.sum()
        if (total == 0L) return MonthlySummary.empty(month)

        val slices = byCategory
            .map { (category, amount) -> CategorySlice(category, amount, amount.toFloat() / total) }
            .sortedByDescending { it.amount }
        return MonthlySummary(month, total, slices)
    }
}

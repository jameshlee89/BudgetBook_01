package com.budgetbook.app.domain

import java.util.Locale

/** [REQ-CHART-003] 금액 표기: 12,500원 */
object MoneyFormat {
    fun won(amount: Long): String = String.format(Locale.KOREA, "%,d원", amount)
}

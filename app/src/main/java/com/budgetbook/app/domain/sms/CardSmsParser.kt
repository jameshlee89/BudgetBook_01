package com.budgetbook.app.domain.sms

import java.time.DateTimeException
import java.time.LocalDateTime

data class ParsedCardSms(
    val cardName: String,
    val amount: Long,
    val dateTime: LocalDateTime,
    val merchant: String,
    /** 0 = 일시불 */
    val installmentMonths: Int,
    val isCancel: Boolean,
)

/**
 * 카드사 승인/취소 문자를 해석한다.
 *
 * [REQ-SMS-002] 주요 카드사 문자에서 카드명, 금액, 일시, 가맹점, 할부를 추출한다.
 * [REQ-SMS-003] 취소 문자는 isCancel=true 로 반환한다.
 * [REQ-SMS-004] 카드 승인/취소 문자가 아니면 null 을 반환한다.
 */
object CardSmsParser {

    private val ISSUER = Regex(
        "(KB국민|국민|NH농협|NH|농협|IBK기업|IBK|기업|신한|삼성|현대|롯데|하나|우리|BC|비씨|씨티|카카오뱅크|토스뱅크|케이뱅크)" +
            "\\s*(카드|체크)\\s*(?:\\(?([0-9*]{4})\\)?)?",
    )
    private val SUMMARY_AMOUNT = Regex("(누적|잔액|한도|사용가능)\\s*:?\\s*[0-9,]+\\s*원")
    private val AMOUNT = Regex("([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)\\s*원")
    private val DATE_TIME = Regex("(\\d{1,2})[/.-](\\d{1,2})\\s*(\\d{1,2}):(\\d{2})")
    private val INSTALLMENT = Regex("(\\d{1,2})\\s*개월")
    private val EXCLUDE_KEYWORDS = listOf("거절", "(광고)", "결제예정", "청구")
    private val MERCHANT_NOISE = listOf(AMOUNT, Regex("일시불"), INSTALLMENT, Regex("승인|취소"))

    const val UNKNOWN_MERCHANT = "미확인 가맹점"

    /**
     * @param receivedAt 문자 수신 시각. 문자에는 연도가 없으므로 연도 추론에 쓴다.
     */
    fun parse(body: String, receivedAt: LocalDateTime): ParsedCardSms? {
        val text = body.replace("[Web발신]", "").replace("\r", "").trim()
        if (EXCLUDE_KEYWORDS.any { text.contains(it) }) return null

        val issuer = ISSUER.find(text) ?: return null
        val isCancel = text.contains("취소")
        if (!isCancel && !text.contains("승인")) return null

        // 누적/잔액 금액을 결제 금액으로 오인하지 않도록 먼저 제거한다.
        val cleaned = SUMMARY_AMOUNT.replace(text, " ")

        val amount = AMOUNT.find(cleaned)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
            ?: return null
        if (amount <= 0) return null

        val dt = DATE_TIME.find(cleaned) ?: return null
        val dateTime = resolveDateTime(dt, receivedAt) ?: return null

        val installment = INSTALLMENT.find(cleaned)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        return ParsedCardSms(
            cardName = cardName(issuer),
            amount = amount,
            dateTime = dateTime,
            merchant = extractMerchant(cleaned.substring(dt.range.last + 1)),
            installmentMonths = installment,
            isCancel = isCancel,
        )
    }

    private fun cardName(match: MatchResult): String {
        val (issuer, kind, digits) = match.destructured
        val base = issuer + kind
        return if (digits.isNotEmpty()) "$base($digits)" else base
    }

    private fun resolveDateTime(match: MatchResult, receivedAt: LocalDateTime): LocalDateTime? {
        val (month, day, hour, minute) = match.destructured.toList().map { it.toInt() }
        return try {
            var candidate = LocalDateTime.of(receivedAt.year, month, day, hour, minute)
            // 12/31 승인 문자를 1/1 에 받은 경우처럼 미래 일시가 되면 전년도로 본다.
            if (candidate.isAfter(receivedAt.plusDays(1))) candidate = candidate.minusYears(1)
            candidate
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun extractMerchant(afterDateTime: String): String {
        for (line in afterDateTime.lines()) {
            var candidate = line
            MERCHANT_NOISE.forEach { candidate = it.replace(candidate, " ") }
            candidate = candidate.replace(Regex("\\s+"), " ").trim()
            if (candidate.isNotEmpty()) return candidate
        }
        return UNKNOWN_MERCHANT
    }
}

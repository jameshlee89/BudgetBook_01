package com.budgetbook.app.domain

import com.budgetbook.app.domain.sms.ParsedCardSms
import java.security.MessageDigest
import java.util.UUID

/**
 * [REQ-SMS-006] 문자 내용에서 결정적 ID 를 만들어 같은 문자를 다시 받거나
 * 문자함에서 다시 가져와도 한 건만 기입되도록 한다.
 * 수신 경로마다 타임스탬프가 달라서 수신 시각이 아니라 해석된 내용으로 만든다.
 */
object TransactionIdGenerator {

    fun fromSms(parsed: ParsedCardSms): String {
        val key = listOf(
            parsed.cardName,
            parsed.amount,
            parsed.dateTime.withSecond(0).withNano(0),
            parsed.merchant,
            parsed.isCancel,
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return "sms_" + digest.take(16).joinToString("") { "%02x".format(it) }
    }

    fun newManualId(): String = "man_" + UUID.randomUUID().toString().replace("-", "")
}

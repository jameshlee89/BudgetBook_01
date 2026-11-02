package com.budgetbook.app.sms

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

data class ImportReport(val scanned: Int, val added: Int, val duplicates: Int)

/** [REQ-SMS-005] 문자함에 이미 있는 선택 월의 카드 문자를 가져온다. */
class SmsInboxImporter(
    private val context: Context,
    private val ingestor: SmsIngestor,
) {
    suspend fun import(month: YearMonth): ImportReport = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        var scanned = 0
        var added = 0
        var duplicates = 0
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} < ?",
            arrayOf(start.toString(), end.toString()),
            "${Telephony.Sms.DATE} ASC",
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                scanned++
                val body = cursor.getString(bodyIndex) ?: continue
                val receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(cursor.getLong(dateIndex)), zone)
                when (ingestor.ingest(body, receivedAt)) {
                    IngestResult.ADDED -> added++
                    IngestResult.DUPLICATE -> duplicates++
                    else -> Unit
                }
            }
        }
        ImportReport(scanned, added, duplicates)
    }
}

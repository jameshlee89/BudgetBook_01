package com.budgetbook.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.budgetbook.app.BudgetBookApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** [REQ-SMS-001] 문자 수신 즉시 카드 승인 내역을 자동 기입한다. */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // 긴 문자는 여러 조각으로 오므로 발신번호별로 합친다.
        val messages = parts.groupBy { it.originatingAddress }.values.map { group ->
            group.joinToString("") { it.messageBody.orEmpty() } to group.first().timestampMillis
        }

        val ingestor = (context.applicationContext as BudgetBookApp).container.smsIngestor
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // goAsync 는 약 10초 안에 끝나야 한다. 놓친 문자는 문자함 가져오기로 복구할 수 있다.
                withTimeoutOrNull(RECEIVER_TIMEOUT_MS) {
                    messages.forEach { (body, timestamp) ->
                        val receivedAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                        val result = ingestor.ingest(body, receivedAt)
                        Log.d(TAG, "ingest: $result")
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SmsReceiver"
        const val RECEIVER_TIMEOUT_MS = 8_000L
    }
}

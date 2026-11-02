package com.budgetbook.app

import android.content.Context
import com.budgetbook.app.data.AuthRepository
import com.budgetbook.app.data.FirestoreLedgerRepository
import com.budgetbook.app.data.LedgerRepository
import com.budgetbook.app.data.SettingsStore
import com.budgetbook.app.sms.SmsInboxImporter
import com.budgetbook.app.sms.SmsIngestor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** 수동 의존성 주입 컨테이너 */
class AppContainer(context: Context) {
    private val firebaseAuth = FirebaseAuth.getInstance()

    val authRepository = AuthRepository(firebaseAuth)
    val ledgerRepository: LedgerRepository = FirestoreLedgerRepository(FirebaseFirestore.getInstance())
    val settings = SettingsStore(context)
    val smsIngestor = SmsIngestor(
        repository = ledgerRepository,
        currentLedgerId = { settings.currentLedgerId },
        currentUserId = { firebaseAuth.currentUser?.uid },
    )
    val inboxImporter = SmsInboxImporter(context, smsIngestor)
}

package com.budgetbook.app.data

import android.content.Context

/** 문자 자동 기입 대상이 되는 현재 가계부를 기억한다. [REQ-SHARE-008] */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("budgetbook_prefs", Context.MODE_PRIVATE)

    var currentLedgerId: String?
        get() = prefs.getString(KEY_CURRENT_LEDGER, null)
        set(value) = prefs.edit().putString(KEY_CURRENT_LEDGER, value).apply()

    private companion object {
        const val KEY_CURRENT_LEDGER = "current_ledger_id"
    }
}

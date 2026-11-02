package com.budgetbook.app.data

import android.util.Log
import com.budgetbook.app.domain.InviteCodeGenerator
import com.budgetbook.app.domain.model.Category
import com.budgetbook.app.domain.model.Ledger
import com.budgetbook.app.domain.model.Transaction
import com.budgetbook.app.domain.model.TransactionSource
import com.budgetbook.app.domain.model.TransactionType
import com.budgetbook.app.domain.model.UserInfo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.tasks.await
import kotlin.math.min
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * Firestore 구조
 *  ledgers/{ledgerId}                  가계부 (members 배열로 접근 제어, firestore.rules 참고)
 *  ledgers/{ledgerId}/transactions/{id} 거래
 *  invites/{code}                      초대 코드 → ledgerId
 *
 * [REQ-SHARE-006] 구성원 모두 같은 문서를 실시간 구독하므로 변경이 즉시 공유된다.
 */
class FirestoreLedgerRepository(
    private val db: FirebaseFirestore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : LedgerRepository {

    private val ledgers get() = db.collection("ledgers")
    private val invites get() = db.collection("invites")
    private fun transactions(ledgerId: String) = ledgers.document(ledgerId).collection("transactions")

    override fun observeLedgers(uid: String): Flow<List<Ledger>> =
        ledgers.whereArrayContains("members", uid).observe("observeLedgers") { snapshot ->
            snapshot.documents.mapNotNull { it.toLedger() }.sortedBy { it.createdAt }
        }

    /**
     * Firestore 리스너는 오류(예: PERMISSION_DENIED)가 한 번 나면 다시 연결되지 않는다.
     * 첫 로그인 직후 기본 가계부가 서버에 기록되기 전에 거래 구독이 시작되면 거부되므로,
     * 오류가 나면 잠시 뒤 다시 구독한다. [REQ-SHARE-006]
     */
    private fun <T> Query.observe(label: String, map: (QuerySnapshot) -> T): Flow<T> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) trySend(map(snapshot))
        }
        awaitClose { registration.remove() }
    }.retryWhen { cause, attempt ->
        Log.w(TAG, "$label failed, retry #${attempt + 1}", cause)
        delay(min(RETRY_BASE_MS shl attempt.toInt().coerceAtMost(6), RETRY_MAX_MS))
        true
    }

    override suspend fun ensureDefaultLedger(user: UserInfo) {
        // 캐시만 보고 판단하면 오프라인 첫 실행 때 기본 가계부가 중복 생성되므로 서버에 묻는다.
        val existing = try {
            ledgers.whereArrayContains("members", user.uid).limit(1).get(Source.SERVER).await()
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "ensureDefaultLedger: offline, skip", e)
            return
        }
        if (existing.isEmpty) createLedger(DEFAULT_LEDGER_NAME, user)
    }

    override suspend fun createLedger(name: String, owner: UserInfo): Ledger {
        repeat(INVITE_CODE_RETRIES) {
            val code = InviteCodeGenerator.generate()
            val ref = ledgers.document()
            val ledger = Ledger(
                id = ref.id,
                name = name,
                ownerUid = owner.uid,
                members = listOf(owner.uid),
                memberNames = mapOf(owner.uid to owner.displayName),
                inviteCode = code,
                createdAt = System.currentTimeMillis(),
            )
            val batch = db.batch()
            batch.set(ref, ledger.toMap())
            batch.set(invites.document(code), mapOf("ledgerId" to ref.id, "createdBy" to owner.uid))
            try {
                batch.commit().await()
                return ledger
            } catch (e: FirebaseFirestoreException) {
                // 이미 쓰이는 코드면 rules 가 invites 덮어쓰기를 거부한다 → 새 코드로 재시도
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        throw IllegalStateException("초대 코드를 만들지 못했습니다.")
    }

    override suspend fun joinLedger(code: String, user: UserInfo): Result<String> = runCatching {
        val normalized = InviteCodeGenerator.normalize(code)
        require(InviteCodeGenerator.isValidFormat(normalized)) { "초대 코드 형식이 올바르지 않습니다." }
        val invite = invites.document(normalized).get(Source.SERVER).await()
        val ledgerId = invite.getString("ledgerId") ?: throw IllegalArgumentException("존재하지 않는 초대 코드입니다.")
        ledgers.document(ledgerId).update(
            mapOf(
                "members" to FieldValue.arrayUnion(user.uid),
                "memberNames.${user.uid}" to user.displayName,
                // rules 에서 초대 코드를 아는 사용자인지 확인하는 데 쓴다.
                "joinedWith" to normalized,
            ),
        ).await()
        ledgerId
    }

    override fun observeTransactions(ledgerId: String, month: YearMonth): Flow<List<Transaction>> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return transactions(ledgerId)
            .whereGreaterThanOrEqualTo("dateTime", start)
            .whereLessThan("dateTime", end)
            .orderBy("dateTime", Query.Direction.DESCENDING)
            .observe("observeTransactions") { snapshot ->
                snapshot.documents
                    .filter { it.getBoolean("deleted") != true }
                    .mapNotNull { it.toTransaction(ledgerId) }
            }
    }

    override suspend fun upsertTransaction(transaction: Transaction) {
        transactions(transaction.ledgerId).document(transaction.id).set(transaction.toMap()).await()
    }

    override suspend fun addIfAbsent(transaction: Transaction): Boolean {
        val ref = transactions(transaction.ledgerId).document(transaction.id)
        val exists = runCatching { ref.get().await().exists() }.getOrDefault(false)
        if (exists) return false
        // 수신기에서 호출되므로 서버 응답을 기다리지 않는다. 로컬 캐시에 기록된 뒤 동기화된다.
        ref.set(transaction.toMap()).addOnFailureListener { Log.w(TAG, "addIfAbsent", it) }
        return true
    }

    override suspend fun deleteTransaction(ledgerId: String, transactionId: String) {
        // soft delete: 문자함을 다시 가져와도 지운 문자 내역이 되살아나지 않게 한다. [REQ-SMS-006]
        transactions(ledgerId).document(transactionId)
            .update(mapOf("deleted" to true, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    private fun Ledger.toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "ownerUid" to ownerUid,
        "members" to members,
        "memberNames" to memberNames,
        "inviteCode" to inviteCode,
        "createdAt" to createdAt,
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toLedger(): Ledger? {
        return Ledger(
            id = id,
            name = getString("name") ?: return null,
            ownerUid = getString("ownerUid") ?: return null,
            members = (get("members") as? List<String>).orEmpty(),
            memberNames = (get("memberNames") as? Map<String, String>).orEmpty(),
            inviteCode = getString("inviteCode").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }

    private fun Transaction.toMap(): Map<String, Any?> = mapOf(
        "amount" to amount,
        "merchant" to merchant,
        "category" to category.name,
        "dateTime" to dateTime.atZone(zone).toInstant().toEpochMilli(),
        "type" to type.name,
        "cardName" to cardName,
        "installmentMonths" to installmentMonths,
        "memo" to memo,
        "source" to source.name,
        "createdBy" to createdBy,
        "manuallyEdited" to manuallyEdited,
        "deleted" to false,
        "updatedAt" to System.currentTimeMillis(),
    )

    private fun DocumentSnapshot.toTransaction(ledgerId: String): Transaction? {
        val millis = getLong("dateTime") ?: return null
        return Transaction(
            id = id,
            ledgerId = ledgerId,
            amount = getLong("amount") ?: return null,
            merchant = getString("merchant").orEmpty(),
            category = Category.fromName(getString("category")),
            dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone),
            type = runCatching { TransactionType.valueOf(getString("type")!!) }.getOrDefault(TransactionType.EXPENSE),
            cardName = getString("cardName"),
            installmentMonths = getLong("installmentMonths")?.toInt() ?: 0,
            memo = getString("memo").orEmpty(),
            source = runCatching { TransactionSource.valueOf(getString("source")!!) }.getOrDefault(TransactionSource.MANUAL),
            createdBy = getString("createdBy").orEmpty(),
            manuallyEdited = getBoolean("manuallyEdited") ?: false,
        )
    }

    companion object {
        private const val TAG = "LedgerRepository"
        private const val INVITE_CODE_RETRIES = 3
        private const val RETRY_BASE_MS = 500L
        private const val RETRY_MAX_MS = 30_000L
        const val DEFAULT_LEDGER_NAME = "내 가계부"
    }
}

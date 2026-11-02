package com.budgetbook.app.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.budgetbook.app.R
import com.budgetbook.app.domain.model.UserInfo
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** 구글 계정 로그인/로그아웃. [REQ-SHARE-001] [REQ-SHARE-002] */
class AuthRepository(private val auth: FirebaseAuth) {

    val currentUser: Flow<UserInfo?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toUserInfo()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun currentUserNow(): UserInfo? = auth.currentUser?.toUserInfo()

    suspend fun signIn(activity: Activity) {
        // google-services.json 에서 생성되는 웹 클라이언트 ID
        val serverClientId = activity.getString(R.string.default_web_client_id)
        val credentialManager = CredentialManager.create(activity)

        val accountPicker = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .build()
        val credential = try {
            credentialManager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(accountPicker).build())
        } catch (e: NoCredentialException) {
            // 기기에 구글 계정이 없으면 계정을 추가/로그인할 수 있는 구글 로그인 화면을 띄운다.
            val signInWithGoogle = GetSignInWithGoogleOption.Builder(serverClientId).build()
            credentialManager.getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(signInWithGoogle).build())
        }.credential

        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "지원하지 않는 로그인 방식입니다."
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    suspend fun signOut(context: Context) {
        auth.signOut()
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }

    private fun FirebaseUser.toUserInfo() = UserInfo(
        uid = uid,
        displayName = displayName ?: email ?: "사용자",
        email = email.orEmpty(),
    )
}

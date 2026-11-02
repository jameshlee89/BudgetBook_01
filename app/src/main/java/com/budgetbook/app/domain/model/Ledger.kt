package com.budgetbook.app.domain.model

/** 여러 구성원이 함께 쓰는 가계부. [REQ-SHARE-003] [REQ-SHARE-006] */
data class Ledger(
    val id: String,
    val name: String,
    val ownerUid: String,
    val members: List<String>,
    val memberNames: Map<String, String>,
    val inviteCode: String,
    val createdAt: Long,
)

data class UserInfo(
    val uid: String,
    val displayName: String,
    val email: String,
)

package com.budgetbook.app.domain

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/**
 * 가계부 초대 코드. [REQ-SHARE-004] [REQ-SHARE-005]
 * 혼동되기 쉬운 문자(0, O, 1, I)는 쓰지 않는다.
 */
object InviteCodeGenerator {

    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 6

    fun generate(random: Random = SecureRandom().asKotlinRandom()): String =
        (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

    /** 사용자가 입력한 코드를 대문자/공백·하이픈 제거 형태로 정규화한다. */
    fun normalize(input: String): String = input.uppercase().filterNot { it.isWhitespace() || it == '-' }

    fun isValidFormat(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}

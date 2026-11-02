package com.budgetbook.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** REQ-SHARE-004, REQ-SHARE-005 */
class InviteCodeGeneratorTest {

    @Test
    fun `TC-SHARE-004-01 초대 코드는 6자리이고 혼동 문자를 쓰지 않음`() {
        val random = Random(42)
        repeat(1_000) {
            val code = InviteCodeGenerator.generate(random)
            assertEquals(6, code.length)
            assertTrue(code, InviteCodeGenerator.isValidFormat(code))
            assertFalse(code, code.any { it in "0O1I" })
        }
    }

    @Test
    fun `TC-SHARE-004-02 초대 코드는 무작위로 분산`() {
        val codes = (1..1_000).map { InviteCodeGenerator.generate() }.toSet()
        // 32^6 경우의 수에서 1,000개는 사실상 겹치지 않는다.
        assertTrue(codes.size >= 999)
    }

    @Test
    fun `TC-SHARE-005-01 입력 코드 정규화와 형식 검사`() {
        assertEquals("ABC234", InviteCodeGenerator.normalize(" abc-234 "))
        assertTrue(InviteCodeGenerator.isValidFormat(InviteCodeGenerator.normalize("abc 234")))
        assertFalse(InviteCodeGenerator.isValidFormat("ABC23"))
        assertFalse(InviteCodeGenerator.isValidFormat("ABC2340"))
        assertFalse(InviteCodeGenerator.isValidFormat("ABCD0I"))
    }
}

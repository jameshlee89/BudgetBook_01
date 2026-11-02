package com.budgetbook.app.domain

import com.budgetbook.app.domain.sms.CardSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** REQ-SMS-002, REQ-SMS-003, REQ-SMS-004 */
class CardSmsParserTest {

    private val received = LocalDateTime.of(2026, 11, 2, 12, 40)

    private fun parse(body: String) = CardSmsParser.parse(body, received)

    @Test
    fun `TC-SMS-002-01 신한카드 단문 승인 문자`() {
        val r = parse("[Web발신]\n신한카드(1234)승인 홍*동 12,500원(일시불)11/02 12:34 스타벅스 강남점 누적1,234,567원")
        assertNotNull(r)
        r!!
        assertEquals("신한카드(1234)", r.cardName)
        assertEquals(12_500L, r.amount)
        assertEquals(LocalDateTime.of(2026, 11, 2, 12, 34), r.dateTime)
        assertEquals("스타벅스 강남점", r.merchant)
        assertEquals(0, r.installmentMonths)
        assertFalse(r.isCancel)
    }

    @Test
    fun `TC-SMS-002-02 삼성카드 장문 승인 문자`() {
        val r = parse("[Web발신]\n삼성카드 승인 홍*동\n45,000원 일시불\n11/01 19:05 교촌치킨\n누적 345,000원")!!
        assertEquals("삼성카드", r.cardName)
        assertEquals(45_000L, r.amount)
        assertEquals(LocalDateTime.of(2026, 11, 1, 19, 5), r.dateTime)
        assertEquals("교촌치킨", r.merchant)
    }

    @Test
    fun `TC-SMS-002-03 KB국민카드 가맹점이 다음 줄에 있는 문자`() {
        val r = parse("[Web발신]\nKB국민카드1234승인\n홍*동님\n8,900원 일시불\n11/02 08:15\nGS25역삼점\n누적1,000,000원")!!
        assertEquals("KB국민카드(1234)", r.cardName)
        assertEquals(8_900L, r.amount)
        assertEquals(LocalDateTime.of(2026, 11, 2, 8, 15), r.dateTime)
        assertEquals("GS25역삼점", r.merchant)
    }

    @Test
    fun `TC-SMS-002-04 현대카드 문자`() {
        val r = parse("[Web발신]\n현대카드 M 승인\n홍*동\n120,000원 일시불\n10/31 21:10\n이마트 성수점\n누적 1,520,000원")!!
        assertEquals("현대카드", r.cardName)
        assertEquals(120_000L, r.amount)
        assertEquals(LocalDateTime.of(2026, 10, 31, 21, 10), r.dateTime)
        assertEquals("이마트 성수점", r.merchant)
    }

    @Test
    fun `TC-SMS-002-05 롯데 하나 우리 NH BC 카드 문자`() {
        val cases = listOf(
            "롯데카드 승인 홍*동 3,000원 일시불 11/02 09:00 롯데시네마" to Triple("롯데카드", 3_000L, "롯데시네마"),
            "하나카드(5678) 홍*동님 15,000원 일시불 승인 11/02 10:00 올리브영" to Triple("하나카드(5678)", 15_000L, "올리브영"),
            "[Web발신]\n우리카드(9012)승인\n홍*동님\n22,000원 일시불\n11/02 11:00\n카카오T" to Triple("우리카드(9012)", 22_000L, "카카오T"),
            "[Web발신]\nNH카드1*2*승인\n홍*동\n5,500원 일시불\n11/02 11:30\n약국" to Triple("NH카드(1*2*)", 5_500L, "약국"),
            "BC카드(3456)승인 홍*동 9,900원 일시불 11/02 12:00 넷플릭스" to Triple("BC카드(3456)", 9_900L, "넷플릭스"),
        )
        cases.forEach { (body, expected) ->
            val r = parse(body)
            assertNotNull(body, r)
            assertEquals(body, expected.first, r!!.cardName)
            assertEquals(body, expected.second, r.amount)
            assertEquals(body, expected.third, r.merchant)
        }
    }

    @Test
    fun `TC-SMS-002-06 할부 개월 수 추출`() {
        val r = parse("신한카드(1234)승인 홍*동 1,200,000원(03개월)11/02 12:00 하이마트 누적2,000,000원")!!
        assertEquals(3, r.installmentMonths)
        assertEquals(1_200_000L, r.amount)
        assertEquals("하이마트", r.merchant)
    }

    @Test
    fun `TC-SMS-002-07 연말 승인 문자를 연초에 받으면 전년도로 해석`() {
        val r = CardSmsParser.parse(
            "신한카드(1234)승인 홍*동 10,000원(일시불)12/31 23:50 편의점",
            LocalDateTime.of(2027, 1, 1, 0, 5),
        )!!
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 50), r.dateTime)
    }

    @Test
    fun `TC-SMS-002-08 체크카드 문자`() {
        val r = parse("[Web발신]\n신한체크(1234)승인 홍*동 6,000원(일시불)11/02 12:10 김밥천국")!!
        assertEquals("신한체크(1234)", r.cardName)
        assertEquals(6_000L, r.amount)
        assertEquals("김밥천국", r.merchant)
    }

    @Test
    fun `TC-SMS-002-09 가맹점이 없으면 미확인 가맹점`() {
        val r = parse("삼성카드 승인 홍*동 7,000원 일시불 11/02 12:20")!!
        assertEquals(CardSmsParser.UNKNOWN_MERCHANT, r.merchant)
    }

    @Test
    fun `TC-SMS-003-01 승인 취소 문자는 취소로 해석`() {
        val r = parse("[Web발신]\n신한카드(1234)승인취소 홍*동 12,500원 11/02 12:34 스타벅스 강남점")!!
        assertTrue(r.isCancel)
        assertEquals(12_500L, r.amount)
        assertEquals("스타벅스 강남점", r.merchant)

        val r2 = parse("삼성카드 취소 홍*동\n45,000원\n11/01 19:05 교촌치킨")!!
        assertTrue(r2.isCancel)
    }

    @Test
    fun `TC-SMS-004-01 일반 문자는 무시`() {
        assertNull(parse("엄마: 저녁 먹고 들어와 11/02 18:00"))
        assertNull(parse("[Web발신] 인증번호 [123456]을 입력해주세요."))
        assertNull(parse("택배가 11/02 12:00 도착 예정입니다. 착불 3,000원"))
    }

    @Test
    fun `TC-SMS-004-02 결제예정 청구 안내는 무시`() {
        assertNull(parse("[Web발신]\n신한카드 11/25 결제예정금액 1,234,560원 입니다."))
        assertNull(parse("[Web발신]\n삼성카드 11월 청구금액 800,000원 (11/14 12:00 기준)"))
    }

    @Test
    fun `TC-SMS-004-03 누적 잔액 금액을 결제 금액으로 오인하지 않음`() {
        val r = parse("[Web발신]\n누적1,234,567원\nKB국민카드1234승인\n홍*동님\n8,900원 일시불\n11/02 08:15\nGS25역삼점")!!
        assertEquals(8_900L, r.amount)
        val r2 = parse("[Web발신]\nNH체크(1234)승인 홍*동 4,500원 11/02 08:30 메가MGC커피 잔액 120,000원")!!
        assertEquals(4_500L, r2.amount)
        assertEquals("메가MGC커피", r2.merchant)
    }

    @Test
    fun `TC-SMS-004-04 승인 거절 문자는 무시`() {
        assertNull(parse("[Web발신]\n신한카드(1234)승인거절 홍*동 50,000원 11/02 12:00 쿠팡 한도초과"))
    }

    @Test
    fun `TC-SMS-004-05 광고 문자는 무시`() {
        assertNull(parse("[Web발신]\n(광고)삼성카드 승인 고객 대상 이벤트! 11/02 12:00 까지 10,000원 캐시백"))
    }
}

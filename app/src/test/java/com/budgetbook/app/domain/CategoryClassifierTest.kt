package com.budgetbook.app.domain

import com.budgetbook.app.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

/** REQ-SMS-007 */
class CategoryClassifierTest {

    @Test
    fun `TC-SMS-007-01 키워드별 항목 분류`() {
        val cases = mapOf(
            "스타벅스 강남점" to Category.CAFE,
            "교촌치킨" to Category.FOOD,
            "카카오T" to Category.TRANSPORT,
            "GS칼텍스 주유소" to Category.TRANSPORT,
            "올리브영" to Category.SHOPPING,
            "이마트 성수점" to Category.LIVING,
            "GS25역삼점" to Category.LIVING,
            "온누리약국" to Category.MEDICAL,
            "CGV왕십리" to Category.CULTURE,
            "넷플릭스" to Category.CULTURE,
            "한국전력공사" to Category.UTILITY,
            "인프런" to Category.EDUCATION,
        )
        cases.forEach { (merchant, expected) -> assertEquals(merchant, expected, CategoryClassifier.classify(merchant)) }
    }

    @Test
    fun `TC-SMS-007-02 우선순위 - 쿠팡이츠는 식비, 쿠팡은 쇼핑`() {
        assertEquals(Category.FOOD, CategoryClassifier.classify("쿠팡이츠"))
        assertEquals(Category.SHOPPING, CategoryClassifier.classify("쿠팡"))
    }

    @Test
    fun `TC-SMS-007-03 분류되지 않으면 기타`() {
        assertEquals(Category.ETC, CategoryClassifier.classify("주식회사 알수없음"))
        assertEquals(Category.ETC, CategoryClassifier.classify("미확인 가맹점"))
    }

    @Test
    fun `TC-SMS-007-04 영문 키워드는 단어 경계로 비교`() {
        assertEquals(Category.TRANSPORT, CategoryClassifier.classify("KTX 승차권"))
        assertEquals(Category.UTILITY, CategoryClassifier.classify("KT 통신요금"))
        assertEquals(Category.LIVING, CategoryClassifier.classify("CU 역삼점"))
        assertEquals(Category.ETC, CategoryClassifier.classify("CUBE STORE"))
    }
}

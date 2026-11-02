package com.budgetbook.app.domain

import com.budgetbook.app.domain.model.Category

/**
 * 가맹점명 키워드로 항목을 자동 분류한다. [REQ-SMS-007]
 *
 * 위에서부터 먼저 일치하는 항목을 쓴다(예: "쿠팡이츠"는 쇼핑보다 식비가 먼저).
 * 영문 키워드는 단어 경계로 비교해 "KTX"가 통신사 "KT"로 분류되지 않게 한다.
 */
object CategoryClassifier {

    private val RULES: List<Pair<Category, List<String>>> = listOf(
        Category.CAFE to listOf(
            "스타벅스", "투썸", "이디야", "커피", "카페", "메가MGC", "빽다방", "컴포즈", "할리스", "폴바셋",
            "베이커리", "파리바게뜨", "뚜레쥬르", "배스킨", "던킨", "설빙",
        ),
        Category.FOOD to listOf(
            "식당", "김밥", "맥도날드", "버거킹", "롯데리아", "맘스터치", "KFC", "치킨", "피자", "배달의민족",
            "배민", "요기요", "쿠팡이츠", "한식", "분식", "국밥", "고기", "초밥", "스시", "반점", "짜장",
        ),
        Category.TRANSPORT to listOf(
            "택시", "카카오T", "버스", "지하철", "코레일", "KTX", "SRT", "주유", "GS칼텍스", "SK에너지",
            "S-OIL", "현대오일", "하이패스", "도로공사", "주차", "티머니",
        ),
        Category.MEDICAL to listOf("병원", "의원", "약국", "치과", "한의원", "안과", "피부과", "정형외과"),
        Category.CULTURE to listOf(
            "CGV", "메가박스", "롯데시네마", "넷플릭스", "유튜브", "멜론", "스포티파이", "영화", "서점",
            "교보문고", "예스24", "노래방", "PC방", "헬스", "호텔", "야놀자", "여기어때",
        ),
        Category.EDUCATION to listOf("학원", "교육", "클래스101", "인프런", "유데미"),
        Category.LIVING to listOf(
            "이마트", "홈플러스", "롯데마트", "코스트코", "마트", "GS25", "CU", "세븐일레븐", "편의점",
            "노브랜드", "하나로",
        ),
        Category.SHOPPING to listOf(
            "쿠팡", "11번가", "G마켓", "지마켓", "옥션", "무신사", "네이버페이", "29CM", "올리브영", "다이소",
            "백화점", "아울렛", "유니클로", "자라",
        ),
        Category.UTILITY to listOf("SKT", "KT", "LGU+", "통신", "전기", "가스", "수도", "관리비", "한국전력"),
    )

    private val MATCHERS: List<Pair<Category, List<(String) -> Boolean>>> = RULES.map { (category, keywords) ->
        category to keywords.map { matcherFor(it.lowercase()) }
    }

    private fun matcherFor(keyword: String): (String) -> Boolean {
        val asciiOnly = keyword.all { it.code < 128 }
        if (!asciiOnly) return { text -> text.contains(keyword) }
        val regex = Regex("(?<![a-z0-9])" + Regex.escape(keyword) + "(?![a-z0-9])")
        return { text -> regex.containsMatchIn(text) }
    }

    fun classify(merchant: String): Category {
        val text = merchant.lowercase()
        return MATCHERS.firstOrNull { (_, matchers) -> matchers.any { it(text) } }?.first ?: Category.ETC
    }
}

package com.budgetbook.app.domain.model

/** 지출 항목. 도넛 그래프의 조각 단위이며 색상은 ARGB. [REQ-CHART-001] */
enum class Category(val label: String, val colorArgb: Long) {
    FOOD("식비", 0xFFE57373),
    CAFE("카페/간식", 0xFFA1887F),
    TRANSPORT("교통", 0xFF64B5F6),
    SHOPPING("쇼핑", 0xFFBA68C8),
    LIVING("생활/마트", 0xFF81C784),
    MEDICAL("의료/건강", 0xFF4DB6AC),
    CULTURE("문화/여가", 0xFFFFB74D),
    UTILITY("통신/공과금", 0xFF90A4AE),
    EDUCATION("교육", 0xFFF06292),
    ETC("기타", 0xFFBDBDBD);

    companion object {
        fun fromName(name: String?): Category = entries.firstOrNull { it.name == name } ?: ETC
    }
}

package com.example.springbootdemo.utils.extensions

/** 전부 숫자인지 (빈 문자열은 false) */
fun String.isNumeric(): Boolean = isNotEmpty() && all { it.isDigit() }

/** null 또는 공백이면 기본값 */
fun String?.orDefault(default: String): String = if (isNullOrBlank()) default else this

/** 앞 [visibleCount]글자만 남기고 마스킹 (예: "홍길동".masked(1) -> "홍**") */
fun String.masked(visibleCount: Int = 3): String =
    if (length <= visibleCount) this else take(visibleCount) + "*".repeat(length - visibleCount)

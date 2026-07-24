package com.example.springbootdemo.domain.vo

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * yyyyMMdd 포맷 날짜 VO (ISO-8601 basic format).
 * 외부 연동 등에서 yyyyMMdd 문자열이 필요할 때 String 대신 이 VO를 사용한다.
 * 내부 로직에서는 [toLocalDate]로 변환해 LocalDate로 다룬다.
 */
@JvmInline
value class BasicDate(
    val value: String,
) {
    init {
        require(value.length == 8 && value.all { it.isDigit() }) { "yyyyMMdd 형식이 아님: $value" }
        LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) // 존재하지 않는 날짜면 예외
    }

    fun toLocalDate(): LocalDate = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)

    override fun toString(): String = value

    companion object {
        fun from(date: LocalDate): BasicDate = BasicDate(date.format(DateTimeFormatter.BASIC_ISO_DATE))
    }
}

package com.example.springbootdemo.utils.extensions

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** "yyyy-MM-dd" 포맷 문자열 */
fun LocalDate.toDateString(): String = format(DATE_FORMATTER)

/** "yyyy-MM-dd HH:mm:ss" 포맷 문자열 */
fun LocalDateTime.toDateTimeString(): String = format(DATE_TIME_FORMATTER)

/** 주말 여부 */
fun LocalDate.isWeekend(): Boolean = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY

/** 두 날짜 사이 일수 (this -> other, 미래면 양수) */
fun LocalDate.daysUntil(other: LocalDate): Long = ChronoUnit.DAYS.between(this, other)

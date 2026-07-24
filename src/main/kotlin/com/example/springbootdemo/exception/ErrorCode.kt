package com.example.springbootdemo.exception

import org.slf4j.event.Level
import org.springframework.http.HttpStatus

/**
 * 에러 코드 정의. 상수명 자체가 코드값이다 (응답의 code 필드 = name).
 * 도메인별 코드는 하단에 추가한다 (형식: <도메인>_<사유>, 예: SAMPLE_NOT_FOUND).
 *
 * - [message]: /api 응답에 그대로 노출되는 워싱된 고객친화 문구 (필수)
 * - [status]: HTTP 상태코드 (필수)
 * - [logLevel]: 이 에러가 찍힐 로그 레벨 (기본 WARN — 정상 흐름에 가까우면 INFO, 심각하면 ERROR)
 * - [debugMessage]: 기본 개발자용 설명. 던질 때 동적 상세(id 등)로 덮어쓸 수 있다.
 */
enum class ErrorCode(
    val message: String,
    val status: HttpStatus,
    val logLevel: Level = Level.WARN,
    val debugMessage: String? = null,
) {
    // 공통
    INVALID_REQUEST("요청이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    NOT_FOUND("요청한 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND, Level.INFO),
    INTERNAL_ERROR("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요", HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR),
    DUMMY_NOT_ALLOWED("허용되지 않은 요청입니다", HttpStatus.FORBIDDEN, Level.ERROR, "dummy 기능은 local/test phase에서만 호출 가능"),

    // Sample 도메인
    SAMPLE_NOT_FOUND("샘플 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND, Level.INFO, "요청한 id의 샘플이 없거나 삭제됨"),
    SAMPLE_ALREADY_DELETED("이미 삭제된 샘플입니다", HttpStatus.CONFLICT, Level.WARN, "이미 소프트딜리트된 샘플에 대한 변경 시도"),
}

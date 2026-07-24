package com.example.springbootdemo.exception

import org.slf4j.event.Level

/**
 * 비즈니스 예외. 서비스는 소비 채널(api/internal/admin)을 모르고 이 예외만 던진다.
 * 응답 노출 수준은 GlobalExceptionHandler가 요청 URI prefix로 결정한다:
 * - /api: 워싱된 errorCode.message만 노출 (debugMessage는 로그 전용)
 * - /internal: 개발자 친화 — debugMessage를 최대한 상세히 노출
 * - /admin: 중간 — 워싱된 메시지 + debugMessage 노출
 *
 * 로그 레벨과 기본 debugMessage는 [ErrorCode]에 정의되어 있고,
 * 던질 때 [debugMessage]로 동적 상세(id, 상태값 등)를 덮어쓸 수 있다.
 * 도메인 전용 예외가 필요하면 이 클래스를 상속한다.
 */
open class BusinessException(
    val errorCode: ErrorCode,
    debugMessage: String? = null,
    cause: Throwable? = null,
) : RuntimeException(debugMessage ?: errorCode.debugMessage ?: errorCode.message, cause) {
    val debugMessage: String? = debugMessage ?: errorCode.debugMessage
    val logLevel: Level get() = errorCode.logLevel
}

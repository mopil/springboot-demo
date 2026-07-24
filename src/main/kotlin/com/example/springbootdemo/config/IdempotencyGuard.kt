package com.example.springbootdemo.config

/**
 * 중복 처리 방지가 꼭 필요한 **비멱등 중요 API**(결제류 등)에 붙이는 가드 표시 (컨트롤러 핸들러 전용, 선택).
 * Swagger에는 SwaggerIdempotencyGuardCustomizer가 배지로 자동 표기한다.
 *
 * 동작 흐름 — 클라이언트 발급 멱등키 + DB 유니크 선점 (레퍼런스: createSample):
 * 1. Request DTO가 IdempotencyRequest를 구현 — 클라이언트가 "시도 1회당 1개"의 UUID를
 *    idempotencyKey 필드로 전송한다 (재시도엔 같은 키 재사용)
 * 2. 엔티티(BaseIdempotencyEntity 상속)가 그 키를 유니크 컬럼으로 저장 — 첫 요청이 키를 선점
 * 3. 재시도(응답 유실 포함)는 같은 키로 오므로: 사전 조회로 409 + 기존 id(debugMessage),
 *    동시 이중 요청은 DB 유니크 제약이 원자적으로 차단(DataIntegrityViolation → 409)
 * 4. 키 미전송(null) 시 서버가 uuid를 발급(fallback)하며 이 경우 중복 가드는 적용되지 않는다 (테스트 편의)
 *
 * @param note 판단 근거·주의사항
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IdempotencyGuard(
    val note: String = "",
)

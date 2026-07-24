package com.example.springbootdemo.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.util.UUID

/**
 * 중복 처리 방지(@IdempotencyGuard)가 필요한 엔티티의 베이스.
 * 클라이언트가 발급한 Idempotency-Key(UUID)를 유니크 컬럼으로 영속한다 —
 * 도메인 데이터와 같은 트랜잭션으로 저장되어 첫 요청이 키를 선점하고,
 * 동시 이중 요청은 유니크 제약이 원자적으로 차단한다 (재시작·다중 인스턴스에도 유지, TTL 불필요).
 *
 * 클라이언트가 키를 안 보내면 서버가 uuid를 발급(fallback)하며, 이 경우 중복 가드는 적용되지 않는다.
 */
@MappedSuperclass
abstract class BaseIdempotencyEntity(
    id: Long = NEW_ID,
    idempotencyKey: String? = null,
) : BaseEntity(id) {
    @Column(nullable = false, unique = true, length = 36, updatable = false)
    val idempotencyKey: String = idempotencyKey ?: UUID.randomUUID().toString()
}

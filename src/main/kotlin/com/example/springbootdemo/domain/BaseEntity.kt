package com.example.springbootdemo.domain

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 모든 엔티티의 공통 베이스 — 엔티티는 반드시 이 클래스를 상속한다.
 * - [id]: PK, 기본값 [NEW_ID](0) + IDENTITY 오토인크리먼트 (저장 시 발급)
 * - [createdAt]/[updatedAt]: JPA Auditing이 자동 관리 (JpaConfig의 @EnableJpaAuditing)
 * - [deletedAt]: 소프트딜리트 마킹 — 불변식 검사는 각 도메인의 delete()에서 하고 [markDeleted]를 호출한다
 *
 * 엔티티는 절대 data class로 선언하지 않는다 (equals/hashCode/copy가 JPA 프록시·가변 상태와 충돌).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = NEW_ID,
) {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    var deletedAt: LocalDateTime? = null
        protected set

    val isDeleted: Boolean get() = deletedAt != null

    /** 소프트딜리트 마킹 — 도메인의 delete()가 불변식 검사 후 호출한다 */
    protected fun markDeleted() {
        deletedAt = LocalDateTime.now()
    }

    companion object {
        /** 신규(저장 전) 엔티티의 id — 저장 시 IDENTITY 전략으로 발급된다 */
        const val NEW_ID = 0L
    }
}

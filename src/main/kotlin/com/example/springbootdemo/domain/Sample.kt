package com.example.springbootdemo.domain

import com.example.springbootdemo.exception.BusinessException
import com.example.springbootdemo.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * rich entity + aggregate root 레퍼런스 도메인.
 * 상태 변경은 반드시 도메인 메서드로만 하고, 불변식은 도메인이 스스로 지킨다.
 * 신규 생성은 [create] 팩토리 → 저장 시 IDENTITY 전략으로 id가 발급된다.
 * 공통 필드(id/createdAt/updatedAt/deletedAt)는 [BaseEntity]가,
 * 중복 처리 가드용 requestId는 [BaseIdempotencyEntity]가 관리한다.
 */
@Entity
@Table(name = "sample")
class Sample(
    name: String,
    memo: String?,
    idempotencyKey: String? = null,
    id: Long = NEW_ID,
) : BaseIdempotencyEntity(id, idempotencyKey) {
    @Column(nullable = false, length = 100)
    var name: String = name
        protected set

    @Column(length = 500)
    var memo: String? = memo
        protected set

    /** 전체 수정 (PUT) */
    fun update(
        name: String,
        memo: String?,
    ) {
        ensureNotDeleted()
        this.name = name
        this.memo = memo
    }

    /** 부분 수정 (PATCH) — null이 아닌 필드만 반영 */
    fun patch(
        name: String?,
        memo: String?,
    ) {
        ensureNotDeleted()
        name?.let { this.name = it }
        memo?.let { this.memo = it }
    }

    /** 소프트딜리트 — 삭제 표시만 하고 데이터는 남긴다 */
    fun delete() {
        ensureNotDeleted()
        markDeleted()
    }

    private fun ensureNotDeleted() {
        if (isDeleted) {
            throw BusinessException(ErrorCode.SAMPLE_ALREADY_DELETED, debugMessage = "id=$id, deletedAt=$deletedAt")
        }
    }

    companion object {
        /** [idempotencyKey]: 클라이언트 발급 멱등키 — null이면 서버가 발급(가드 미적용) */
        fun create(
            name: String,
            memo: String?,
            idempotencyKey: String? = null,
        ): Sample = Sample(name = name, memo = memo, idempotencyKey = idempotencyKey)
    }
}

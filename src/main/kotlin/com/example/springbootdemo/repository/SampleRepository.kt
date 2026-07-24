package com.example.springbootdemo.repository

import com.example.springbootdemo.domain.Sample
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.Repository

/**
 * Spring Data가 구현을 자동 생성한다 (별도 구현 클래스 없음).
 * JpaRepository 통상속 금지 — 마커 Repository를 상속하고 **필요한 메서드만 선언**해서
 * 소프트딜리트를 우회하는 메서드(deleteAll, 삭제분 포함 findAll 등)가 노출되지 않게 한다.
 */
interface SampleRepository : Repository<Sample, Long> {
    /** aggregate root 단위 저장. 신규(id == NEW_ID)면 IDENTITY로 id가 발급된다. */
    fun save(sample: Sample): Sample

    /** 소프트딜리트 제외 단건 조회 */
    fun findByIdAndDeletedAtIsNull(id: Long): Sample?

    /** 소프트딜리트 제외 페이징 조회 (totalElements 포함) */
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Sample>

    /**
     * 중복 처리 가드 — 해당 멱등키로 이미 처리(저장)된 엔티티 조회.
     * **소프트딜리트 포함이 의도다** (처리 "이력" 기준 — 삭제된 데이터의 키로도 재생성을 막아야 함).
     * 다른 조회처럼 DeletedAtIsNull을 붙이지 말 것.
     */
    fun findByIdempotencyKey(idempotencyKey: String): Sample?
}

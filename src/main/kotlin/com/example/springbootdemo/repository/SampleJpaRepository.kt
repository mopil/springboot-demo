package com.example.springbootdemo.repository

import com.example.springbootdemo.domain.Sample
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA 전용 보조 인터페이스.
 * Service에서 직접 주입 금지 — 반드시 SampleRepository(도메인 인터페이스)를 통해 사용한다.
 */
interface SampleJpaRepository : JpaRepository<Sample, Long> {
    fun findByIdAndDeletedAtIsNull(id: Long): Sample?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Sample>

    fun countByDeletedAtIsNull(): Long
}

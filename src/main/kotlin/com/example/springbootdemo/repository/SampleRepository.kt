package com.example.springbootdemo.repository

import com.example.springbootdemo.domain.Sample
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

interface SampleRepository {
    /** aggregate root 단위 저장. 신규(id == NEW_ID)면 id를 발급해 반환한다. */
    fun save(sample: Sample): Sample

    /** 소프트딜리트된 데이터는 제외 */
    fun findById(id: Long): Sample?

    /** 소프트딜리트 제외 + id 오름차순 페이징 */
    fun findAll(
        page: Int,
        size: Int,
    ): List<Sample>

    /** 소프트딜리트 제외 전체 건수 */
    fun count(): Long

    /** 기본 구현체 — Spring Data JPA 위임 (SampleJpaRepository는 이 클래스만 주입한다) */
    @Primary
    @Repository
    class Jpa(
        private val sampleJpaRepository: SampleJpaRepository,
    ) : SampleRepository {
        override fun save(sample: Sample): Sample = sampleJpaRepository.save(sample)

        override fun findById(id: Long): Sample? = sampleJpaRepository.findByIdAndDeletedAtIsNull(id)

        override fun findAll(
            page: Int,
            size: Int,
        ): List<Sample> =
            sampleJpaRepository
                .findAllByDeletedAtIsNull(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")))
                .content

        override fun count(): Long = sampleJpaRepository.countByDeletedAtIsNull()
    }

    /** 인메모리 구현체 — DB 없이 돌려야 할 때 Jpa의 @Primary를 옮겨서 교체 */
    @Repository
    class InMemory : SampleRepository {
        private val store = ConcurrentHashMap<Long, Sample>()
        private val sequence = AtomicLong(0)

        override fun save(sample: Sample): Sample {
            val saved =
                if (sample.id == Sample.NEW_ID) {
                    Sample(id = sequence.incrementAndGet(), name = sample.name, memo = sample.memo)
                } else {
                    sample
                }
            store[saved.id] = saved
            return saved
        }

        override fun findById(id: Long): Sample? = store[id]?.takeUnless { it.isDeleted }

        override fun findAll(
            page: Int,
            size: Int,
        ): List<Sample> =
            store.values
                .filterNot { it.isDeleted }
                .sortedBy { it.id }
                .drop(page * size)
                .take(size)

        override fun count(): Long = store.values.count { !it.isDeleted }.toLong()
    }
}

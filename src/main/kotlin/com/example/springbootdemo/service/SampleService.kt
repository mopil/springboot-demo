package com.example.springbootdemo.service

import com.example.springbootdemo.domain.Sample
import com.example.springbootdemo.exception.BusinessException
import com.example.springbootdemo.exception.ErrorCode
import com.example.springbootdemo.repository.SampleRepository
import com.example.springbootdemo.service.component.ValidateName
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface SampleService {
    /** 이름은 공백 불가 (ValidateName). 신규 id는 저장 시 발급된다. */
    fun create(command: CreateCommand): Sample

    /** 비즈니스적으로 중요한 단건 조회 — 미존재/삭제된 샘플이면 SAMPLE_NOT_FOUND 예외 */
    fun get(id: Long): Sample

    /** 조회성 — 예외 없이 빈 목록 허용. 소프트딜리트된 샘플은 제외된다. */
    fun getPage(
        page: Int,
        size: Int,
    ): PageResult

    /** 전체 필드 교체 (PUT). 삭제된 샘플은 수정 불가 (SAMPLE_ALREADY_DELETED). */
    fun put(command: PutCommand): Sample

    /** null이 아닌 필드만 반영 (PATCH). 삭제된 샘플은 수정 불가. */
    fun patch(command: PatchCommand): Sample

    /** 소프트딜리트 — deletedAt 마킹만 하고 데이터는 유지. 이후 조회에서 제외된다. */
    fun delete(id: Long): Sample

    data class CreateCommand(
        val name: String,
        val memo: String?,
    )

    data class PutCommand(
        val id: Long,
        val name: String,
        val memo: String?,
    )

    data class PatchCommand(
        val id: Long,
        val name: String?,
        val memo: String?,
    )

    data class PageResult(
        val samples: List<Sample>,
        val totalElements: Long,
    )

    @Service
    @Transactional(readOnly = true)
    class Default(
        private val sampleRepository: SampleRepository,
        private val validateName: ValidateName,
    ) : SampleService {
        private val log = LoggerFactory.getLogger(javaClass)

        @Transactional
        override fun create(command: CreateCommand): Sample {
            validateName(command.name)
            val saved = sampleRepository.save(Sample.create(name = command.name, memo = command.memo))
            log.info("샘플 생성: id=${saved.id}, name=${saved.name}")
            return saved
        }

        override fun get(id: Long): Sample = findOrThrow(id)

        override fun getPage(
            page: Int,
            size: Int,
        ): PageResult =
            PageResult(
                samples = sampleRepository.findAll(page = page, size = size),
                totalElements = sampleRepository.count(),
            )

        @Transactional
        override fun put(command: PutCommand): Sample {
            validateName(command.name)
            val sample = findOrThrow(command.id)
            sample.update(name = command.name, memo = command.memo)
            log.info("샘플 전체 수정: id=${sample.id}, name=${command.name}")
            return sampleRepository.save(sample)
        }

        @Transactional
        override fun patch(command: PatchCommand): Sample {
            command.name?.let { validateName(it) }
            val sample = findOrThrow(command.id)
            sample.patch(name = command.name, memo = command.memo)
            log.info("샘플 부분 수정: id=${sample.id}, name=${command.name}, memo=${command.memo}")
            return sampleRepository.save(sample)
        }

        @Transactional
        override fun delete(id: Long): Sample {
            val sample = findOrThrow(id)
            sample.delete()
            log.info("샘플 소프트딜리트: id=${sample.id}, deletedAt=${sample.deletedAt}")
            return sampleRepository.save(sample)
        }

        private fun findOrThrow(id: Long): Sample =
            sampleRepository.findById(id)
                ?: throw BusinessException(ErrorCode.SAMPLE_NOT_FOUND, debugMessage = "Sample not found: id=$id")
    }
}

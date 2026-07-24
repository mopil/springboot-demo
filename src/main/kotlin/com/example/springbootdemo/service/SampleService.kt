package com.example.springbootdemo.service

import com.example.springbootdemo.domain.Sample
import com.example.springbootdemo.exception.BusinessException
import com.example.springbootdemo.exception.ErrorCode
import com.example.springbootdemo.repository.SampleRepository
import com.example.springbootdemo.service.component.ValidateName
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface SampleService {
    /**
     * 이름은 공백 불가 (ValidateName). 신규 id는 저장 시 발급된다.
     * 중복 처리 가드: [CreateCommand.idempotencyKey]가 오면 그 키를 유니크 컬럼으로 선점 저장한다 —
     * 같은 키 재시도는 409(기존 id 포함), 동시 이중 요청은 유니크 제약이 차단한다.
     */
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
        /** 클라이언트 발급 멱등키 (Idempotency-Key 헤더 → 컨트롤러가 채움, 재시도엔 같은 키) */
        val idempotencyKey: String? = null,
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
            command.idempotencyKey?.let { key ->
                sampleRepository.findByIdempotencyKey(key)?.let { existing ->
                    throw BusinessException(
                        ErrorCode.DUPLICATE_REQUEST,
                        debugMessage = "이미 처리된 생성 요청: idempotencyKey=$key, 기존 샘플 id=${existing.id}",
                    )
                }
            }
            validateName(command.name)
            val saved =
                try {
                    sampleRepository.save(Sample.create(name = command.name, memo = command.memo, idempotencyKey = command.idempotencyKey))
                } catch (e: DataIntegrityViolationException) {
                    // 멱등키 유니크 충돌(동시 이중 요청)로 한정 — 클라 키가 없으면 무관한 무결성 위반이므로 그대로 전파
                    if (command.idempotencyKey == null) throw e
                    throw BusinessException(
                        ErrorCode.DUPLICATE_REQUEST,
                        debugMessage = "동시 중복 생성 요청: idempotencyKey=${command.idempotencyKey}",
                        cause = e,
                    )
                }
            log.info("샘플 생성: id=${saved.id}, name=${saved.name}, idempotencyKey=${saved.idempotencyKey}")
            return saved
        }

        override fun get(id: Long): Sample = findOrThrow(id)

        override fun getPage(
            page: Int,
            size: Int,
        ): PageResult {
            val result =
                sampleRepository.findAllByDeletedAtIsNull(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")),
                )
            return PageResult(samples = result.content, totalElements = result.totalElements)
        }

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
            sampleRepository.findByIdAndDeletedAtIsNull(id)
                ?: throw BusinessException(ErrorCode.SAMPLE_NOT_FOUND, debugMessage = "Sample not found: id=$id")
    }
}

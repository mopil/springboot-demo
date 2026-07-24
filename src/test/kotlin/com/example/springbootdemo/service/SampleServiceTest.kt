package com.example.springbootdemo.service

import com.example.springbootdemo.domain.Sample
import com.example.springbootdemo.exception.BusinessException
import com.example.springbootdemo.exception.ErrorCode
import com.example.springbootdemo.repository.SampleRepository
import com.example.springbootdemo.service.component.ValidateName
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class SampleServiceTest {
    private val sampleRepository = mockk<SampleRepository>()
    private val sampleService = SampleService.Default(sampleRepository, ValidateName.Default())

    @Test
    fun `create - 저장 후 id가 발급된 도메인을 반환한다`() {
        every { sampleRepository.save(any()) } answers {
            val sample = firstArg<Sample>()
            Sample(id = 1L, name = sample.name, memo = sample.memo)
        }

        val result = sampleService.create(SampleService.CreateCommand(name = "kakao", memo = "메모"))

        result.id shouldBe 1L
        result.name shouldBe "kakao"
        result.memo shouldBe "메모"
        verify(exactly = 1) { sampleRepository.save(any()) }
    }

    @Test
    fun `create - 이미 처리된 멱등키면 DUPLICATE_REQUEST를 던지고 저장하지 않는다`() {
        every { sampleRepository.findByIdempotencyKey("dup-key") } returns
            Sample(id = 7L, name = "kakao", memo = null, idempotencyKey = "dup-key")

        val e =
            shouldThrow<BusinessException> {
                sampleService.create(SampleService.CreateCommand(name = "kakao", memo = null, idempotencyKey = "dup-key"))
            }

        e.errorCode shouldBe ErrorCode.DUPLICATE_REQUEST
        verify(exactly = 0) { sampleRepository.save(any()) }
    }

    @Test
    fun `create - 삭제된 샘플의 멱등키로 재요청해도 DUPLICATE_REQUEST를 던진다 (처리 이력 기준)`() {
        val deleted = Sample(id = 7L, name = "kakao", memo = null, idempotencyKey = "deleted-key")
        deleted.delete()
        every { sampleRepository.findByIdempotencyKey("deleted-key") } returns deleted

        val e =
            shouldThrow<BusinessException> {
                sampleService.create(SampleService.CreateCommand(name = "kakao", memo = null, idempotencyKey = "deleted-key"))
            }

        e.errorCode shouldBe ErrorCode.DUPLICATE_REQUEST
    }

    @Test
    fun `create - 처리 이력 없는 멱등키면 그 키를 선점해 저장한다`() {
        every { sampleRepository.findByIdempotencyKey("new-key") } returns null
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.create(SampleService.CreateCommand(name = "kakao", memo = null, idempotencyKey = "new-key"))

        result.idempotencyKey shouldBe "new-key"
    }

    @Test
    fun `create - 동시 이중 요청으로 유니크 충돌이 나면 DUPLICATE_REQUEST로 변환한다`() {
        every { sampleRepository.findByIdempotencyKey("race-key") } returns null
        every { sampleRepository.save(any()) } throws DataIntegrityViolationException("unique violation")

        val e =
            shouldThrow<BusinessException> {
                sampleService.create(SampleService.CreateCommand(name = "kakao", memo = null, idempotencyKey = "race-key"))
            }

        e.errorCode shouldBe ErrorCode.DUPLICATE_REQUEST
    }

    @Test
    fun `create - 이름이 공백이면 INVALID_REQUEST를 던지고 저장하지 않는다`() {
        val e =
            shouldThrow<BusinessException> {
                sampleService.create(SampleService.CreateCommand(name = " ", memo = null))
            }

        e.errorCode shouldBe ErrorCode.INVALID_REQUEST
        verify(exactly = 0) { sampleRepository.save(any()) }
    }

    @Test
    fun `get - 존재하면 도메인을 반환한다`() {
        every { sampleRepository.findByIdAndDeletedAtIsNull(1L) } returns Sample(id = 1L, name = "kakao", memo = null)

        val result = sampleService.get(1L)

        result.id shouldBe 1L
        result.name shouldBe "kakao"
    }

    @Test
    fun `get - 없으면 SAMPLE_NOT_FOUND를 던진다`() {
        every { sampleRepository.findByIdAndDeletedAtIsNull(99L) } returns null

        val e =
            shouldThrow<BusinessException> {
                sampleService.get(99L)
            }
        e.errorCode shouldBe ErrorCode.SAMPLE_NOT_FOUND
    }

    @Test
    fun `getPage - 목록과 전체 건수를 함께 반환한다`() {
        every { sampleRepository.findAllByDeletedAtIsNull(any()) } returns
            PageImpl(
                listOf(
                    Sample(id = 1L, name = "a", memo = null),
                    Sample(id = 2L, name = "b", memo = null),
                ),
                PageRequest.of(0, 2),
                5L,
            )

        val result = sampleService.getPage(page = 0, size = 2)

        result.samples.map { it.name } shouldBe listOf("a", "b")
        result.totalElements shouldBe 5L
    }

    @Test
    fun `put - 전체 필드를 교체한다`() {
        every { sampleRepository.findByIdAndDeletedAtIsNull(1L) } returns Sample(id = 1L, name = "before", memo = "old")
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.put(SampleService.PutCommand(id = 1L, name = "after", memo = null))

        result.name shouldBe "after"
        result.memo shouldBe null
    }

    @Test
    fun `patch - null이 아닌 필드만 반영한다`() {
        every { sampleRepository.findByIdAndDeletedAtIsNull(1L) } returns Sample(id = 1L, name = "before", memo = "keep")
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.patch(SampleService.PatchCommand(id = 1L, name = "after", memo = null))

        result.name shouldBe "after"
        result.memo shouldBe "keep"
    }

    @Test
    fun `delete - 소프트딜리트로 deletedAt을 마킹한다`() {
        every { sampleRepository.findByIdAndDeletedAtIsNull(1L) } returns Sample(id = 1L, name = "kakao", memo = null)
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.delete(1L)

        result.deletedAt.shouldNotBeNull()
        result.isDeleted shouldBe true
    }

    @Test
    fun `delete - 이미 삭제된 샘플은 도메인 불변식이 SAMPLE_ALREADY_DELETED를 던진다`() {
        val deleted = Sample(id = 1L, name = "kakao", memo = null)
        deleted.delete()
        every { sampleRepository.findByIdAndDeletedAtIsNull(1L) } returns deleted

        val e =
            shouldThrow<BusinessException> {
                sampleService.delete(1L)
            }
        e.errorCode shouldBe ErrorCode.SAMPLE_ALREADY_DELETED
    }
}

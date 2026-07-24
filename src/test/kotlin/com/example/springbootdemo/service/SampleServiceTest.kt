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
        every { sampleRepository.findById(1L) } returns Sample(id = 1L, name = "kakao", memo = null)

        val result = sampleService.get(1L)

        result.id shouldBe 1L
        result.name shouldBe "kakao"
    }

    @Test
    fun `get - 없으면 SAMPLE_NOT_FOUND를 던진다`() {
        every { sampleRepository.findById(99L) } returns null

        val e =
            shouldThrow<BusinessException> {
                sampleService.get(99L)
            }
        e.errorCode shouldBe ErrorCode.SAMPLE_NOT_FOUND
    }

    @Test
    fun `getPage - 목록과 전체 건수를 함께 반환한다`() {
        every { sampleRepository.findAll(page = 0, size = 2) } returns
            listOf(
                Sample(id = 1L, name = "a", memo = null),
                Sample(id = 2L, name = "b", memo = null),
            )
        every { sampleRepository.count() } returns 5L

        val result = sampleService.getPage(page = 0, size = 2)

        result.samples.map { it.name } shouldBe listOf("a", "b")
        result.totalElements shouldBe 5L
    }

    @Test
    fun `put - 전체 필드를 교체한다`() {
        every { sampleRepository.findById(1L) } returns Sample(id = 1L, name = "before", memo = "old")
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.put(SampleService.PutCommand(id = 1L, name = "after", memo = null))

        result.name shouldBe "after"
        result.memo shouldBe null
    }

    @Test
    fun `patch - null이 아닌 필드만 반영한다`() {
        every { sampleRepository.findById(1L) } returns Sample(id = 1L, name = "before", memo = "keep")
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.patch(SampleService.PatchCommand(id = 1L, name = "after", memo = null))

        result.name shouldBe "after"
        result.memo shouldBe "keep"
    }

    @Test
    fun `delete - 소프트딜리트로 deletedAt을 마킹한다`() {
        every { sampleRepository.findById(1L) } returns Sample(id = 1L, name = "kakao", memo = null)
        every { sampleRepository.save(any()) } answers { firstArg() }

        val result = sampleService.delete(1L)

        result.deletedAt.shouldNotBeNull()
        result.isDeleted shouldBe true
    }

    @Test
    fun `delete - 이미 삭제된 샘플은 도메인 불변식이 SAMPLE_ALREADY_DELETED를 던진다`() {
        val deleted = Sample(id = 1L, name = "kakao", memo = null)
        deleted.delete()
        every { sampleRepository.findById(1L) } returns deleted

        val e =
            shouldThrow<BusinessException> {
                sampleService.delete(1L)
            }
        e.errorCode shouldBe ErrorCode.SAMPLE_ALREADY_DELETED
    }
}

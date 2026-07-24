package com.example.springbootdemo.controller.dto

import com.example.springbootdemo.domain.Sample
import com.example.springbootdemo.service.SampleService
import com.example.springbootdemo.utils.extensions.toDateTimeString
import io.swagger.v3.oas.annotations.media.Schema

// object 명 = 컨트롤러 핸들러 메서드명, 내부 클래스명은 항상 Request/Response (목록 요소는 Item)

object CreateSample {
    @Schema(name = "CreateSampleRequest", description = "샘플 생성 요청")
    data class Request(
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
        @field:Schema(description = "클라이언트 발급 멱등키 (UUID v4, 시도 1회당 1개 — 재시도엔 같은 키). null이면 서버 발급 + 가드 미적용", nullable = true)
        override val idempotencyKey: String?,
    ) : IdempotencyRequest {
        fun toCommand(): SampleService.CreateCommand =
            SampleService.CreateCommand(name = name, memo = memo, idempotencyKey = idempotencyKey)
    }

    @Schema(name = "CreateSampleResponse", description = "샘플 생성 응답")
    data class Response(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
        @field:Schema(description = "이 생성을 처리한 멱등키 (클라 발급 키 또는 서버 fallback 발급)", nullable = false)
        val idempotencyKey: String,
    ) {
        companion object {
            fun from(sample: Sample): Response =
                Response(id = sample.id, name = sample.name, memo = sample.memo, idempotencyKey = sample.idempotencyKey)
        }
    }
}

object GetSample {
    @Schema(name = "GetSampleResponse", description = "샘플 단건 조회 응답")
    data class Response(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
    ) {
        companion object {
            fun from(sample: Sample): Response = Response(id = sample.id, name = sample.name, memo = sample.memo)
        }
    }
}

object GetPageSamples {
    @Schema(name = "GetPageSamplesItem", description = "샘플 목록 항목")
    data class Item(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
    ) {
        companion object {
            fun from(sample: Sample): Item = Item(id = sample.id, name = sample.name, memo = sample.memo)
        }
    }
}

object PutSample {
    @Schema(name = "PutSampleRequest", description = "샘플 전체 수정 요청 (PUT)")
    data class Request(
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
    ) {
        fun toCommand(id: Long): SampleService.PutCommand = SampleService.PutCommand(id = id, name = name, memo = memo)
    }

    @Schema(name = "PutSampleResponse", description = "샘플 전체 수정 응답")
    data class Response(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
    ) {
        companion object {
            fun from(sample: Sample): Response = Response(id = sample.id, name = sample.name, memo = sample.memo)
        }
    }
}

object PatchSample {
    @Schema(name = "PatchSampleRequest", description = "샘플 부분 수정 요청 (PATCH) — null 필드는 수정하지 않음")
    data class Request(
        @field:Schema(description = "이름 (미수정 시 null)", nullable = true)
        val name: String?,
        @field:Schema(description = "메모 (미수정 시 null)", nullable = true)
        val memo: String?,
    ) {
        fun toCommand(id: Long): SampleService.PatchCommand = SampleService.PatchCommand(id = id, name = name, memo = memo)
    }

    @Schema(name = "PatchSampleResponse", description = "샘플 부분 수정 응답")
    data class Response(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
    ) {
        companion object {
            fun from(sample: Sample): Response = Response(id = sample.id, name = sample.name, memo = sample.memo)
        }
    }
}

object DeleteSample {
    @Schema(name = "DeleteSampleResponse", description = "샘플 삭제(소프트딜리트) 응답")
    data class Response(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
        @field:Schema(description = "삭제 시각 (yyyy-MM-dd HH:mm:ss)", nullable = false)
        val deletedAt: String,
    ) {
        companion object {
            fun from(sample: Sample): Response =
                Response(
                    id = sample.id,
                    deletedAt = requireNotNull(sample.deletedAt).toDateTimeString(),
                )
        }
    }
}

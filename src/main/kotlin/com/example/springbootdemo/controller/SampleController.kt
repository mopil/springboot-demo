package com.example.springbootdemo.controller

import com.example.springbootdemo.config.ApiPath
import com.example.springbootdemo.config.IdempotencyGuard
import com.example.springbootdemo.controller.dto.CreateSample
import com.example.springbootdemo.controller.dto.DeleteSample
import com.example.springbootdemo.controller.dto.ErrorResponse
import com.example.springbootdemo.controller.dto.GetPageSamples
import com.example.springbootdemo.controller.dto.GetSample
import com.example.springbootdemo.controller.dto.PageResponse
import com.example.springbootdemo.controller.dto.PatchSample
import com.example.springbootdemo.controller.dto.PutSample
import com.example.springbootdemo.service.SampleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Sample", description = "샘플 API — 규약 레퍼런스 구현 (rich domain + 소프트딜리트 + 페이징)")
@RestController
@RequestMapping("${ApiPath.API}/samples")
class SampleController(
    private val sampleService: SampleService,
) {
    @Operation(summary = "생성")
    @IdempotencyGuard(note = "클라이언트가 시도 1회당 1개의 UUID를 발급해 재시도에 같은 키를 재사용하면 중복 생성이 차단된다")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "성공"),
        ApiResponse(
            responseCode = "409",
            description = "DUPLICATE_REQUEST — 이미 처리된 Idempotency-Key로 재요청",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSample(
        @RequestBody request: CreateSample.Request,
    ): CreateSample.Response = CreateSample.Response.from(sampleService.create(request.toCommand()))

    @Operation(summary = "단건 조회", description = "비즈니스적으로 중요한 단건 조회라 미존재 시 예외를 내린다")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공"),
        ApiResponse(
            responseCode = "404",
            description = "SAMPLE_NOT_FOUND — 샘플이 없거나 삭제됨",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @GetMapping("/{id}")
    fun getSample(
        @PathVariable id: Long,
    ): GetSample.Response = GetSample.Response.from(sampleService.get(id))

    @Operation(summary = "목록 조회 (페이징)", description = "조회성 API — 예외 없이 빈 목록으로 응답. 소프트딜리트 제외.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공 (없으면 빈 목록)"),
    )
    @GetMapping
    fun getPageSamples(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<GetPageSamples.Item> {
        val result = sampleService.getPage(page = page, size = size)
        return PageResponse.of(
            content = result.samples.map(GetPageSamples.Item::from),
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }

    @Operation(summary = "전체 수정 (PUT)", description = "멱등 — 전체 필드 교체라 같은 요청 반복 시 상태 동일")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공"),
        ApiResponse(
            responseCode = "404",
            description = "SAMPLE_NOT_FOUND — 샘플이 없거나 삭제됨",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PutMapping("/{id}")
    fun putSample(
        @PathVariable id: Long,
        @RequestBody request: PutSample.Request,
    ): PutSample.Response = PutSample.Response.from(sampleService.put(request.toCommand(id)))

    @Operation(summary = "부분 수정 (PATCH)", description = "멱등 — null이 아닌 필드만 치환 반영한다 (증분 아님)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공"),
        ApiResponse(
            responseCode = "404",
            description = "SAMPLE_NOT_FOUND — 샘플이 없거나 삭제됨",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @PatchMapping("/{id}")
    fun patchSample(
        @PathVariable id: Long,
        @RequestBody request: PatchSample.Request,
    ): PatchSample.Response = PatchSample.Response.from(sampleService.patch(request.toCommand(id)))

    @Operation(summary = "삭제 (소프트딜리트)", description = "상태 멱등 — deletedAt 마킹만 하고 데이터는 유지 (재호출 응답은 404)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "성공"),
        ApiResponse(
            responseCode = "404",
            description = "SAMPLE_NOT_FOUND — 샘플이 없거나 이미 삭제됨",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))],
        ),
    )
    @DeleteMapping("/{id}")
    fun deleteSample(
        @PathVariable id: Long,
    ): DeleteSample.Response = DeleteSample.Response.from(sampleService.delete(id))
}

package com.example.springbootdemo.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "SliceResponse", description = "공통 슬라이스 응답 (무한스크롤용 — 전체 건수 없이 다음 페이지 존재 여부만)")
data class SliceResponse<T>(
    @field:Schema(description = "데이터 목록", nullable = false)
    val content: List<T>,
    @field:Schema(description = "페이지 번호 (0부터 시작)", nullable = false)
    val page: Int,
    @field:Schema(description = "페이지 크기", nullable = false)
    val size: Int,
    @field:Schema(description = "다음 페이지 존재 여부", nullable = false)
    val hasNext: Boolean,
) {
    companion object {
        /**
         * size + 1개를 조회한 목록을 넘기면 hasNext를 판단하고 초과분을 잘라낸다.
         * 예: repository에서 limit = size + 1로 조회 → of(rows, page, size)
         */
        fun <T> of(
            content: List<T>,
            page: Int,
            size: Int,
        ): SliceResponse<T> =
            SliceResponse(
                content = content.take(size),
                page = page,
                size = size,
                hasNext = content.size > size,
            )
    }
}

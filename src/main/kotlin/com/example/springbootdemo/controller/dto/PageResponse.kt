package com.example.springbootdemo.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "PageResponse", description = "공통 페이지 응답 (전체 건수 포함)")
data class PageResponse<T>(
    @field:Schema(description = "데이터 목록", nullable = false)
    val content: List<T>,
    @field:Schema(description = "페이지 번호 (0부터 시작)", nullable = false)
    val page: Int,
    @field:Schema(description = "페이지 크기", nullable = false)
    val size: Int,
    @field:Schema(description = "전체 건수", nullable = false)
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", nullable = false)
    val totalPages: Int,
) {
    companion object {
        fun <T> of(
            content: List<T>,
            page: Int,
            size: Int,
            totalElements: Long,
        ): PageResponse<T> =
            PageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = if (size <= 0) 0 else ((totalElements + size - 1) / size).toInt(),
            )
    }
}

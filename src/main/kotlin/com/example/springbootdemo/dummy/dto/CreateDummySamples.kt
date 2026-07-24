package com.example.springbootdemo.dummy.dto

import io.swagger.v3.oas.annotations.media.Schema

object CreateDummySamples {
    @Schema(name = "CreateDummySamplesResponse", description = "샘플 더미 생성 응답")
    data class Response(
        @field:Schema(description = "생성된 건수", nullable = false)
        val count: Int,
        @field:Schema(description = "생성된 샘플 ID 목록", nullable = false)
        val ids: List<Long>,
    )
}

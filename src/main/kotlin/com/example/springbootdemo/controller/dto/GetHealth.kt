package com.example.springbootdemo.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

object GetHealth {
    @Schema(name = "GetHealthResponse", description = "헬스체크 응답")
    data class Response(
        @field:Schema(description = "서버 상태", nullable = false)
        val status: String,
    )
}

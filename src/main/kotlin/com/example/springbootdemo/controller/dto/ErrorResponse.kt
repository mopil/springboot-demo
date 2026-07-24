package com.example.springbootdemo.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "ErrorResponse", description = "공통 에러 응답")
data class ErrorResponse(
    @field:Schema(description = "에러 코드", nullable = false)
    val code: String,
    @field:Schema(description = "에러 메시지 (/api는 워싱된 고객친화 메시지)", nullable = false)
    val message: String,
    @field:Schema(description = "디버그 메시지 (internal/admin 예외만, /api 응답에는 항상 null)", nullable = true)
    val debugMessage: String? = null,
)

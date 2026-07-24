package com.example.springbootdemo.controller

import com.example.springbootdemo.controller.dto.ErrorResponse
import com.example.springbootdemo.exception.ErrorCode
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/**
 * 모든 API에 공통 에러 응답(400 INVALID_REQUEST, 500 INTERNAL_ERROR)을 자동 명시한다.
 * 핸들러에는 해당 API 고유 에러(404 등)만 @ApiResponses로 수동 명시하면 된다.
 */
@Component
class SwaggerCommonErrorCustomizer :
    OperationCustomizer,
    OpenApiCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        addIfAbsent(operation, "400", "${ErrorCode.INVALID_REQUEST.name} — 요청 형식 오류 (공통)")
        addIfAbsent(operation, "500", "${ErrorCode.INTERNAL_ERROR.name} — 서버 오류 (공통)")
        return operation
    }

    override fun customise(openApi: OpenAPI) {
        val components = openApi.components ?: Components().also { openApi.components = it }
        ModelConverters
            .getInstance()
            .read(ErrorResponse::class.java)
            .forEach { (name, schema) -> components.addSchemas(name, schema) }
    }

    private fun addIfAbsent(
        operation: Operation,
        status: String,
        description: String,
    ) {
        if (operation.responses.containsKey(status)) return
        operation.responses.addApiResponse(
            status,
            ApiResponse()
                .description(description)
                .content(
                    Content().addMediaType(
                        "application/json",
                        MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/ErrorResponse")),
                    ),
                ),
        )
    }
}

package com.example.springbootdemo.config.swagger

import com.example.springbootdemo.config.IdempotencyGuard
import io.swagger.v3.oas.models.Operation
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

/** @IdempotencyGuard를 읽어 Swagger operation description에 가드 배지를 자동 표기한다. */
@Component
class SwaggerIdempotencyGuardCustomizer : OperationCustomizer {
    override fun customize(
        operation: Operation,
        handlerMethod: HandlerMethod,
    ): Operation {
        val guard = handlerMethod.getMethodAnnotation(IdempotencyGuard::class.java) ?: return operation
        // 전역 + 그룹 양쪽에서 호출돼도 중복 적용 방지
        if (operation.description?.contains(BADGE) == true) return operation

        val line = if (guard.note.isNotBlank()) "$BADGE — ${guard.note}" else BADGE
        operation.description = listOfNotNull(line, operation.description).joinToString("\n\n")
        return operation
    }

    companion object {
        private const val BADGE = "🛡️ [비멱등 · 중복 처리 가드]"
    }
}

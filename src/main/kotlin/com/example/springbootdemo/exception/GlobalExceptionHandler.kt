package com.example.springbootdemo.exception

import com.example.springbootdemo.config.ApiPath
import com.example.springbootdemo.controller.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * 공통 예외 처리.
 * - BusinessException: ErrorCode의 logLevel로 로깅하고, 요청 URI prefix별로 노출 수준을 결정한다.
 * - Spring MVC 프레임워크 예외(400/404/405 등): ResponseEntityExceptionHandler가 원래 상태코드로
 *   처리하되, 응답 바디는 공통 ErrorResponse로 변환한다.
 * - 그 외 예외: 500 + INTERNAL_ERROR 워싱 응답.
 * - internal/admin 채널은 프레임워크/미처리 예외에도 debugMessage를 함께 내린다.
 */
@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(
        e: BusinessException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        // 정상 흐름에 가까운 비즈니스 예외는 스택트레이스 없이 한 줄, 원인 예외가 있을 때만 스택 포함
        log
            .atLevel(e.logLevel)
            .let { if (e.cause != null) it.setCause(e) else it }
            .log("BusinessException: code=${e.errorCode.name}, debug=${e.debugMessage}")

        val path = request.path()
        val body =
            when {
                // internal: 개발자 친화 — debugMessage를 최대한 상세히 노출
                path.matchesPrefix(ApiPath.INTERNAL) ->
                    ErrorResponse(
                        code = e.errorCode.name,
                        message = e.debugMessage ?: e.errorCode.message,
                        debugMessage = e.debugMessage,
                    )
                // admin: 중간 — 워싱된 메시지 + debugMessage 함께
                path.matchesPrefix(ApiPath.ADMIN) ->
                    ErrorResponse(
                        code = e.errorCode.name,
                        message = e.errorCode.message,
                        debugMessage = e.debugMessage,
                    )
                // api(및 그 외): 워싱된 고객친화 메시지만, debugMessage는 로그 전용
                else ->
                    ErrorResponse(
                        code = e.errorCode.name,
                        message = e.errorCode.message,
                    )
            }
        return ResponseEntity.status(e.errorCode.status).body(body)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpected(
        e: Exception,
        request: HttpServletRequest,
    ): ErrorResponse {
        log.error("Unexpected exception", e)
        return ErrorResponse(
            code = ErrorCode.INTERNAL_ERROR.name,
            message = ErrorCode.INTERNAL_ERROR.message,
            debugMessage = request.path().internalDebugMessageOrNull(e),
        )
    }

    // 프레임워크 예외(타입 미스매치 400, 미존재 경로 404, 미지원 메서드 405 등)를
    // 원래 상태코드 그대로 공통 ErrorResponse 포맷으로 변환한다
    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (statusCode.is5xxServerError) {
            // 5xx 프레임워크 예외(직렬화 실패 등)는 서버 결함이므로 스택 포함 error
            log.error("Framework exception: status=${statusCode.value()}", ex)
        } else {
            log.warn("Framework exception: status=${statusCode.value()}, type=${ex.javaClass.simpleName}, message=${ex.message}")
        }
        val errorCode =
            when {
                statusCode.value() == HttpStatus.NOT_FOUND.value() -> ErrorCode.NOT_FOUND
                statusCode.is4xxClientError -> ErrorCode.INVALID_REQUEST
                else -> ErrorCode.INTERNAL_ERROR
            }
        val path = (request as? ServletWebRequest)?.request?.path().orEmpty()
        return ResponseEntity
            .status(statusCode)
            .headers(headers)
            .body(
                ErrorResponse(
                    code = errorCode.name,
                    message = errorCode.message,
                    debugMessage = path.internalDebugMessageOrNull(ex),
                ),
            )
    }

    private fun HttpServletRequest.path(): String = requestURI.removePrefix(contextPath)

    // internal/admin 채널에는 개발자용 상세를 함께 내린다
    private fun String.internalDebugMessageOrNull(ex: Exception): String? =
        if (matchesPrefix(ApiPath.INTERNAL) || matchesPrefix(ApiPath.ADMIN)) {
            "${ex.javaClass.simpleName}: ${ex.message}"
        } else {
            null
        }

    // /internal-report 같은 오매칭을 막기 위한 세그먼트 단위 prefix 매칭
    private fun String.matchesPrefix(prefix: String): Boolean = this == prefix || startsWith("$prefix/")
}

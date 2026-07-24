package com.example.springbootdemo.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger 그룹 설정.
 * 주의: GroupedOpenApi를 쓰면 전역 customizer 빈이 그룹 문서에 자동 적용되지 않으므로
 * 모든 OperationCustomizer/OpenApiCustomizer 빈을 각 그룹에 명시 등록한다.
 */
@Configuration
class OpenApiConfig(
    private val operationCustomizers: List<OperationCustomizer>,
    private val openApiCustomizers: List<OpenApiCustomizer>,
) {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Spring Boot Demo API")
                    .description("데모 API")
                    .version("v1"),
            )

    @Bean
    fun apiGroup(): GroupedOpenApi = group(id = "1-api", displayName = "API (외부)", prefix = ApiPath.API)

    @Bean
    fun internalGroup(): GroupedOpenApi = group(id = "2-internal", displayName = "Internal (서버 간)", prefix = ApiPath.INTERNAL)

    @Bean
    fun adminGroup(): GroupedOpenApi = group(id = "3-admin", displayName = "Admin (어드민)", prefix = ApiPath.ADMIN)

    @Bean
    fun dummyGroup(): GroupedOpenApi = group(id = "4-dummy", displayName = "Dummy (local/test 전용)", prefix = ApiPath.DUMMY)

    private fun group(
        id: String,
        displayName: String,
        prefix: String,
    ): GroupedOpenApi {
        val builder =
            GroupedOpenApi
                .builder()
                .group(id)
                .displayName(displayName)
                .pathsToMatch("$prefix/**")
        operationCustomizers.forEach { builder.addOperationCustomizer(it) }
        openApiCustomizers.forEach { builder.addOpenApiCustomizer(it) }
        return builder.build()
    }
}

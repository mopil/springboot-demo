package com.example.springbootdemo.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
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
    fun apiGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("1-api")
            .displayName("API (외부)")
            .pathsToMatch("${ApiPath.API}/**")
            .build()

    @Bean
    fun internalGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("2-internal")
            .displayName("Internal (서버 간)")
            .pathsToMatch("${ApiPath.INTERNAL}/**")
            .build()

    @Bean
    fun adminGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("3-admin")
            .displayName("Admin (어드민)")
            .pathsToMatch("${ApiPath.ADMIN}/**")
            .build()

    @Bean
    fun dummyGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("4-dummy")
            .displayName("Dummy (local/test 전용)")
            .pathsToMatch("${ApiPath.DUMMY}/**")
            .build()
}

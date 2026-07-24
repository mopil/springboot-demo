package com.example.springbootdemo.controller

import com.example.springbootdemo.config.ApiPath
import com.example.springbootdemo.controller.dto.GetHealth
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Health", description = "헬스체크 API")
@RestController
@RequestMapping("${ApiPath.INTERNAL}/health")
class HealthController {
    @Operation(summary = "헬스체크")
    @GetMapping
    fun getHealth(): GetHealth.Response = GetHealth.Response(status = "UP")
}

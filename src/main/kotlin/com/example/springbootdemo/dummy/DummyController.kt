package com.example.springbootdemo.dummy

import com.example.springbootdemo.config.ApiPath
import com.example.springbootdemo.dummy.dto.CreateDummySamples
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Dummy", description = "더미데이터 생성 API (local/test 전용)")
@DummyOnly
@RestController
@RequestMapping("${ApiPath.DUMMY}/samples")
class DummyController(
    private val sampleServiceDummyGenerator: SampleServiceDummyGenerator,
) {
    @Operation(summary = "샘플 더미 생성", description = "실제 서비스 로직으로 더미데이터를 생성한다. local/test phase 전용.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createDummySamples(
        @RequestParam(defaultValue = "10") count: Int,
    ): CreateDummySamples.Response {
        val samples = sampleServiceDummyGenerator.generate(count)
        return CreateDummySamples.Response(count = samples.size, ids = samples.map { it.id })
    }
}

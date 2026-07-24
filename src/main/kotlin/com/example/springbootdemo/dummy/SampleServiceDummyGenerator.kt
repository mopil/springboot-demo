package com.example.springbootdemo.dummy

import com.example.springbootdemo.domain.Sample
import com.example.springbootdemo.service.SampleService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Sample 더미데이터 생성기 — Swagger 호출 테스트 용이를 위해 실제 서비스 로직으로 더미를 만든다.
 * @DummyOnly에 @Profile("local | test")가 포함되어 local/test phase에서만 빈이 생성된다.
 */
@DummyOnly
@Component
class SampleServiceDummyGenerator(
    private val sampleService: SampleService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(count: Int): List<Sample> {
        log.info("샘플 더미 생성 시작: count=$count")
        return (1..count).map { index ->
            sampleService.create(
                SampleService.CreateCommand(name = "샘플-$index", memo = "더미 데이터 $index"),
            )
        }
    }
}

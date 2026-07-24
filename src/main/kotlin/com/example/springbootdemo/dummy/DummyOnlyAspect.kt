package com.example.springbootdemo.dummy

import com.example.springbootdemo.exception.BusinessException
import com.example.springbootdemo.exception.ErrorCode
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component

/**
 * @DummyOnly 이중 안전장치 — 프로파일 조건 없이 항상 등록되어,
 * @Profile 제한이 실수로 풀리더라도 live에서 dummy 로직 실행을 차단한다.
 */
@Aspect
@Component
class DummyOnlyAspect(
    private val environment: Environment,
) {
    @Before(
        "@within(com.example.springbootdemo.dummy.DummyOnly) || " +
            "@annotation(com.example.springbootdemo.dummy.DummyOnly)",
    )
    fun verifyDummyAllowed() {
        if (!environment.acceptsProfiles(Profiles.of("local", "test"))) {
            throw BusinessException(
                ErrorCode.DUMMY_NOT_ALLOWED,
                debugMessage = "dummy 기능 차단: activeProfiles=${environment.activeProfiles.joinToString()}",
            )
        }
    }
}

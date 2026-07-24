package com.example.springbootdemo.dummy

import org.springframework.context.annotation.Profile

/**
 * dummy 기능 표시 어노테이션. 이거 하나로 두 가지가 적용된다:
 * 1. @Profile("local | test") 메타 어노테이션 — live에서는 빈 자체가 생성되지 않는다.
 * 2. DummyOnlyAspect(AOP)가 호출을 가로채 local/test phase가 아니면 차단한다 (이중 안전장치).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Profile("local | test")
annotation class DummyOnly

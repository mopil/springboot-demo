package com.example.springbootdemo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

// JUnit 테스트는 프로파일 미지정 → 기본 local phase로 뜬다 (test phase는 배포 검증계 전용)
@SpringBootTest
class SpringbootDemoApplicationTests {
    @Test
    fun contextLoads() {
    }
}

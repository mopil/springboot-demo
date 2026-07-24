package com.example.springbootdemo.service.component

import com.example.springbootdemo.exception.BusinessException
import com.example.springbootdemo.exception.ErrorCode
import org.springframework.stereotype.Component

/**
 * 여러 Service가 공유하는 invokable 컴포넌트 샘플.
 * 원칙: 1가지 일만 하는 fun interface + nested Default 구현체.
 */
fun interface ValidateName {
    operator fun invoke(name: String)

    @Component
    class Default : ValidateName {
        override fun invoke(name: String) {
            if (name.isBlank()) {
                throw BusinessException(ErrorCode.INVALID_REQUEST, debugMessage = "name이 공백: '$name'")
            }
        }
    }
}

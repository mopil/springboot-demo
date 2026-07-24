package com.example.springbootdemo.controller.dto

/**
 * 중복 처리 가드(@IdempotencyGuard) 대상 API의 Request가 구현하는 인터페이스.
 * 구현하면 "이 API는 멱등키를 받는다"가 타입 레벨에 드러난다.
 *
 * 클라이언트는 시도 1회당 1개의 UUID를 발급해 [idempotencyKey]에 담고,
 * 재시도(더블클릭·타임아웃 재전송 포함)에는 같은 키를 재사용한다.
 * null이면 서버가 발급(fallback)하며 이 경우 중복 가드는 적용되지 않는다.
 */
interface IdempotencyRequest {
    val idempotencyKey: String?
}

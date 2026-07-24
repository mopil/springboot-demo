package com.example.springbootdemo.config

/**
 * API URL prefix 규약.
 * - API: 클라이언트로 나가는 외부 API
 * - INTERNAL: 서버 to 서버 호출 API (인증 불필요)
 * - ADMIN: 어드민 API (인증은 현재 스코프 밖 — 추후 필요 시 추가)
 * - DUMMY: 더미데이터 생성 API (local/test phase 전용 — live에선 빈 자체가 없음 + AOP 차단)
 */
object ApiPath {
    const val API = "/api"
    const val INTERNAL = "/internal"
    const val ADMIN = "/admin"
    const val DUMMY = "/dummy"
}

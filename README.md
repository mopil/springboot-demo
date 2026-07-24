# springboot-demo

**요구사항을 던지면 휴먼 개입을 최소화하고 품질 좋은 결과만 받는 것**을 목표로 하는 Spring Boot 스켈레톤.
라이브코딩 테스트 베이스로 설계됐다 — 판단이 필요한 지점을 규약으로 미리 제거해서, AI가 흔들림 없이 일관된 산출물을 내게 한다.

## 기술 스택

| 구분 | 사용 |
|---|---|
| Framework | Spring Boot 4, Spring Data JPA (H2, MySQL 모드) |
| Language | Kotlin 2.3 (Java 17) |
| API 문서 | springdoc-openapi (Swagger UI: `/swagger`) |
| 테스트 | JUnit5 + kotest(assertion) + mockk, Kover 커버리지 게이트(서비스 레이어 70%) |
| 품질 강제 | **ArchUnit 13규칙**(계층/네이밍/prefix/응답 형태), ktlint |
| 빌드 | Gradle (버전은 `gradle.properties` 일괄 관리) |

## 빠른 시작

```bash
./gradlew bootRun                    # 기본 local phase → http://localhost:8080/swagger
./gradlew ktlintFormat ktlintCheck test koverVerify   # 포맷 → 린트 → 테스트 → 커버리지 (구현 후 필수)
```

더미데이터 생성 후 Swagger에서 바로 호출해볼 수 있다:

```bash
curl -X POST "http://localhost:8080/dummy/samples?count=10"
```

## 아키텍처 요약

```
controller (dto)  →  service (component, Command/Result)  →  repository (JPA/InMemory)  →  domain (rich entity, vo)
                              ↕ exception (ErrorCode enum), dummy (local/test 전용), utils/extensions
```

- **계층 간 DTO 공유 금지**: Request → `toCommand()` → 도메인 반환 → `Response.from()` 조립
- **rich entity + aggregate root**: 비즈니스 로직·불변식은 도메인 메서드가 소유, 서비스는 오케스트레이션만
- **예외 체계**: 서비스는 채널 중립 `BusinessException(ErrorCode)`만 던지고, 핸들러가 URL prefix로 노출 수준 결정
  - `/api` 워싱된 고객 메시지만 · `/internal` debugMessage 상세 · `/admin` 중간 · `/dummy` local/test 전용(3중 안전장치)
- **소프트딜리트 원칙**, PUT/PATCH 구분, 페이징(`PageResponse`/`SliceResponse`), 핸들러 명명 통일(get/getPage/put/patch/delete)
- **프로파일 3-phase**: local(개발) / test(검증계) / live(운영) — 데이터소스는 `application-db.properties` 분리, logback은 stdout 중심 + 수집기 확장 슬롯

전체 규약은 **[CLAUDE.md](./CLAUDE.md)** 에 있다. `Sample` 도메인이 모든 규약의 레퍼런스 구현이므로, 새 도메인은 Sample 패턴을 복제하면 된다.

## AI 협업 워크플로우

1. 요구사항 → `.claude/tech-spec/`에 테크스펙 작성
2. 구현 (단위테스트 + 더미 생성기 포함)
3. `.claude/skills/code-review` 스킬을 서브에이전트로 실행 — **Pn 룰 기반 리뷰, P1이 0이 될 때까지 반복**
4. 포맷·린트·테스트·커버리지 검증 → 요구사항 단위 커밋
5. `/commit-pr` 스킬로 푸시·PR (tech-spec 링크 포함)

# springboot-demo

범용 Spring Boot 스켈레톤 프로젝트. 라이브코딩 테스트 베이스로 재사용 가능 (Spring Boot 4 + Kotlin, Java 17, JPA + H2).

**이 프로젝트의 정체성: 요구사항을 던지면 휴먼 개입을 최소화하고 품질 좋은 결과만 받을 수 있게 하는 스켈레톤이다.** 규약이 촘촘한 이유가 이것이다 — 판단이 필요한 지점을 규약으로 미리 없애서, AI가 흔들림 없이 일관된 산출물을 내게 한다. 규약에 없는 판단 지점을 만나면 임의로 정하지 말고 tech-spec에 기록하고 사용자에게 확인하라. **Sample 도메인(도메인/리포지토리/서비스/컨트롤러/DTO/테스트)이 모든 규약의 레퍼런스 구현이다 — 새 도메인은 Sample 패턴을 그대로 복제하라.**

## 아키텍처 규약

### 패키지 구조

```
com.example.springbootdemo
├── config      # 설정 (OpenAPI, ApiPath 등)
│   └── swagger    # Swagger 문서화 지원 customizer (공통 에러/멱등 가드 배지 — controller.dto.ErrorResponse 참조 예외)
├── controller  # REST 컨트롤러
│   └── dto     # request/response DTO + 공통 ErrorResponse (controller 계층 전용)
├── service     # 서비스 인터페이스 (+ 내부 기본 구현체, Command/Result)
│   ├── component  # 여러 Service가 공유하는 invokable 컴포넌트
│   └── dto     # (필요 시) service 전용 dto/vo
├── repository  # Spring Data 리포지토리 인터페이스 (마커 Repository 상속, 구현 자동 생성)
│   └── dto     # (필요 시) 조회 전용 projection 등
├── domain      # 도메인 모델 (= JPA 엔티티, BaseEntity 상속, rich entity)
│   └── vo      # 공용 값 객체 (BasicDate 등 — 특정 도메인에 속하지 않는 VO)
├── exception   # 공통 예외 (BusinessException, ErrorCode enum, + GlobalExceptionHandler)
├── dummy       # 더미데이터 생성 (local/test 전용 — 생성기/컨트롤러/@DummyOnly AOP)
└── utils
    └── extensions  # 커스텀 확장함수 (StringExtension.kt, LocalDateExtension.kt 등)
```

**DTO는 별도 최상위 패키지가 아니라 각 계층 하위의 `dto` 패키지에 둔다.**

**도메인이 늘어나도 도메인별 하위 패키지는 만들지 않는다** — 플랫 구조를 유지하고 파일명 prefix(`Sample~`, `Order~`)로 구분한다 (2~4개 도메인 규모에선 이게 가장 빠르고, ArchUnit 규칙과도 충돌 없음).

### API prefix 규약 (필수)

모든 컨트롤러 매핑은 `config/ApiPath` 상수의 prefix로 시작한다 (**ArchUnit으로 강제됨**). Swagger 그룹도 prefix별로 분리되어 있다 (`OpenApiConfig`의 `GroupedOpenApi`). 헬스체크는 `${ApiPath.INTERNAL}/health`.

| prefix | 상수 | 용도 |
|---|---|---|
| `/api` | `ApiPath.API` | 클라이언트로 나가는 외부 API |
| `/internal` | `ApiPath.INTERNAL` | 서버 to 서버 호출 API (인증 불필요) |
| `/admin` | `ApiPath.ADMIN` | 어드민 API (인증은 현재 스코프 밖 — 추후 필요 시 추가, 요구가 오면 tech-spec 먼저) |
| `/dummy` | `ApiPath.DUMMY` | 더미데이터 생성 API (local/test phase 전용) |

```kotlin
@RequestMapping("${ApiPath.API}/orders")   // 문자열 더하기(+) 대신 $ 템플릿 사용
```

### API 설계 규약 (필수)

1. **응답은 항상 JSON 객체로 내린다** — 최상위가 배열(`List<...>`)인 응답 금지. 목록은 `PageResponse`/`SliceResponse`로 감싸거나, Response 내부의 리스트 필드로 감싼다 (요소는 Response 내부 nested class, 예: `Response(tests: List<Item>)`).
2. **빈 응답이 내려가는 경우를 없앤다** — 반환 타입 `Response?` 금지, 바디 없는 200/204 금지, **`ResponseEntity` 반환 금지**(ArchUnit 강제 — Response DTO를 직접 반환하고 상태코드는 `@ResponseStatus`로). 삭제/수정처럼 돌려줄 데이터가 없어도 최소한 식별자·상태를 담은 Response를 내린다.
3. **발생 가능한 예외와 에러코드를 Swagger에 명시한다** — 공통 에러(400 `INVALID_REQUEST`, 500 `INTERNAL_ERROR`)는 `SwaggerCommonErrorCustomizer`가 전 API에 자동 부착하므로, 핸들러에는 **그 API 고유 에러(404 등)만** `@ApiResponses`로 명시한다 (content 스키마는 `ErrorResponse`).
4. **조회성 API는 원칙적으로 예외를 내지 않는다** (조회 실패가 호출측 서비스 크래시로 번지는 것 방지) — 목록은 빈 리스트, 옵셔널한 데이터는 기본값으로 응답. 단, **비즈니스적으로 조회 데이터가 중요한 경우**(결제/정산 정보 등 없으면 진행 불가)는 예외 허용.
5. **전체 데이터 수정은 `PUT`, 일부 데이터 수정은 `PATCH`**로 매핑한다.
6. **삭제는 `DELETE` 매핑 + 소프트딜리트가 원칙** — 도메인에 삭제 표시(`deletedAt` 등)를 두고 조회에서 제외한다. 하드딜리트는 고려하지 않는다.
7. **중복 처리 가드 — 꼭 필요한 중요 API(비멱등 결제류 등)에만 적용한다** (필수 아님, 레퍼런스: `createSample`). 방식: **클라이언트 발급 Idempotency-Key + DB 유니크 선점**:
   - **클라이언트가 "시도 1회당 1개"의 UUID(v4)를 발급**해 Request 바디의 `idempotencyKey` 필드로 전송한다 — Request DTO가 **`IdempotencyRequest` 인터페이스를 구현**해 가드 대상임이 타입에 드러나게 한다. 재시도(더블클릭·타임아웃 재전송)에는 같은 키를 재사용하고, 성공/새 시도에만 새 키를 발급한다.
   - 대상 엔티티는 **`BaseIdempotencyEntity`를 상속** — 그 키를 `idempotencyKey` **유니크 컬럼**으로 저장해 첫 요청이 키를 선점한다 (도메인 데이터와 같은 트랜잭션 영속 — 재시작·다중 인스턴스에도 유지, TTL 불필요).
   - 검사 2중: ① 서비스가 `findByIdempotencyKey` 사전 조회 → 있으면 **409 DUPLICATE_REQUEST** (debugMessage에 기존 id 포함), ② 동시 이중 요청은 **DB 유니크 제약이 원자적으로 차단** — `DataIntegrityViolationException`을 catch해 409로 변환한다. **응답 유실 후 재시도까지 차단된다** (같은 키로 오므로).
   - catch 범위 주의: **클라 키가 있을 때만** 409로 변환하고 그 외 무결성 위반은 그대로 전파한다. 엔티티에 다른 유니크 제약이 추가되면 제약명 검사로 더 좁힐 것. 이 패턴은 IDENTITY 전략(저장 즉시 insert → 그 자리에서 예외) 전제다 — SEQUENCE로 바꾸면 예외가 커밋 시점으로 밀려 catch를 빗나간다.
   - `findByIdempotencyKey`는 **소프트딜리트 포함이 의도** (처리 "이력" 기준) — `DeletedAtIsNull`을 붙이지 말 것.
   - 키가 null이면 서버가 발급(fallback)하고 **가드는 적용되지 않는다** (테스트 편의 — 응답 바디의 `idempotencyKey`로 확인 가능).
   - 핸들러에는 `@IdempotencyGuard(note = ...)`(`config/IdempotencyGuard`)를 붙인다 — Swagger에 가드 배지 자동 표기(`SwaggerIdempotencyGuardCustomizer`) + 409 응답을 `@ApiResponses`에 명시.
   - 멱등 API(PUT/PATCH/DELETE 등)는 가드 불필요 — 멱등 특성은 `@Operation` description에 적는다. 완전한 재시도 UX(409 대신 기존 리소스 응답 재생 200)가 필요하면 사전 조회 분기에서 확장한다.
   - 주의: `GroupedOpenApi`를 쓰므로 **Swagger customizer는 그룹 빌더에 명시 등록**해야 적용된다 (`OpenApiConfig` 참고 — 전역 빈 등록만으론 그룹 문서에 안 나온다).
8. **핸들러 메서드 명명은 prefix로 통일**한다 (object 명도 이와 동일):

| prefix | 용도 | 예 |
|---|---|---|
| `create` | 생성 | `createSample` |
| `get` | 단건 조회 | `getSample` |
| `getAll` | 비페이징 리스트 조회 | `getAllSamples` |
| `getPage` | 페이징 조회 (`PageResponse`) | `getPageSamples` |
| `getSlice` | 무한스크롤 조회 (`SliceResponse`) | `getSliceSamples` |
| `put` | 전체 수정 | `putSample` |
| `patch` | 부분 수정 | `patchSample` |
| `delete` | 삭제 (소프트딜리트) | `deleteSample` |

```kotlin
@Operation(summary = "단건 조회")
@ApiResponses(
    ApiResponse(responseCode = "200", description = "성공"),
    ApiResponse(
        responseCode = "404",
        description = "TEST_NOT_FOUND — 테스트 정보를 찾을 수 없음",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    ),
)
```

### 계층 규약 (필수 — ArchUnit으로 강제됨)

`src/test/kotlin/.../ArchitectureTest.kt`가 아래 규칙을 검증한다. 테스트가 깨지면 코드를 규약에 맞춰라 (테스트를 고치지 마라).

- **참조 방향: controller → service → repository 단방향.** 역방향 참조 금지. 아주 간단한 조회 등은 controller → repository 직접 참조도 허용.
- **계층 간 DTO 공유 금지.** 중복이 생기더라도 계층별 모델을 유지한다:
  - **controller**: request/response DTO(`controller/dto`)는 controller 계층 전용. 요청은 `request.toCommand()`로 service Command로 변환해 넘기고, service가 반환한 도메인을 `Response.from(domain)`으로 조립한다.
  - **service**: 입력은 **Command**, 서비스 전용 반환 모델은 **Result로 통일** (Info/Output 등 다른 명명 금지) — 둘 다 **해당 서비스 인터페이스 하위에 nested data class로 정의**해 응집력 있게 둔다 (예: `FooService.CreateCommand`, `FooService.CreateResult`). 단순 반환은 도메인(엔티티) 그대로. controller의 request/response를 받지도 반환하지도 않는다.
  - **repository**: 도메인(또는 `repository/dto`의 조회 전용 projection)만 반환한다. **저장은 aggregate root 도메인 단위(`save(domain)`)로 통일**하고, 조회 파라미터(id, page, size 등)는 원시 값 가능. Command나 controller DTO를 알지 못한다.
- **타 애그리거트 데이터가 필요하면 해당 Repository를 직접 주입해 id로 조회하는 것을 허용**한다 (애그리거트 간 id 참조 규약의 자연 연장). Service 간 참조 금지 규칙과 혼동하지 말 것 — Service는 못 부르지만 Repository는 부를 수 있다.
- **트랜잭션 규약**: Service 구현체(`Default`) 클래스 레벨에 `@Transactional(readOnly = true)`를 붙이고, **쓰기가 필요한 메서드에만** 메서드 레벨 `@Transactional`을 덧붙인다.
- **과도한 컴포넌트 분해 금지**: 신규 구현 시 로직을 잘게 쪼개지 말고 **일단 Service에 응집**시켜라 (응집된 코드가 리뷰·가독성에 유리). 컴포넌트화는 **여러 Service에서 재사용이 실제로 보이는 것만** 고민한다.
- **전략 패턴**: 하나의 역할에 서브 구현체가 여러 개 나올 수 있는 것(결제수단별 처리, 할인 정책 등)은 인터페이스 + 구현체 목록 주입(`List<FooStrategy>`) 방식의 전략 패턴으로 구현한다. if/when으로 타입 분기하지 않는다.
- **서비스 인터페이스 메서드에는 KDoc으로 비즈니스 규약·동작 원리를 적는다** (예: "삭제된 샘플은 수정 불가"). 시그니처만 봐도 뻔한 내용은 적지 않는다.
- **예외 처리**: 아래 예외 규약을 따른다.

### 예외 규약 (필수)

- **서비스는 소비 채널을 모른다.** `exception/BusinessException`(`RuntimeException` 상속)에 `ErrorCode` + `debugMessage`(개발자용 상세)를 담아 던지기만 한다. 도메인 전용 예외가 필요하면 BusinessException을 상속한다.
- **로그 레벨과 기본 debugMessage는 `ErrorCode` enum에 정의한다.** `ErrorCode(message, status, logLevel, debugMessage?)` — 핸들러가 `errorCode.logLevel`로 로깅한다 (기본 `WARN`, 미존재 조회 등 정상 흐름에 가까우면 `INFO`, 심각하면 `ERROR`). 던질 때 `BusinessException(errorCode, debugMessage = "...")`로 동적 상세를 덮어쓸 수 있다.
- **노출 수준은 `exception/GlobalExceptionHandler`가 요청 URI prefix로 결정한다** (= prefix 기반 워싱):
  - **`/api`** (외부 고객): **워싱된 고객친화 메시지(`errorCode.message`)만** 노출. `debugMessage`는 로그 전용.
  - **`/internal`** (서버 간): 개발자 친화 — **`debugMessage`를 최대한 상세히** 응답에 그대로 노출.
  - **`/admin`** (어드민): 중간 — 워싱된 메시지 + `debugMessage` 함께 노출.
- **에러 코드는 `exception/ErrorCode` enum에 정의**한다 (`ErrorCode(message, status)`). **enum 상수명 자체가 코드값**이며(응답 `code` = `name`), 도메인별 코드는 `<도메인>_<사유>` 형식 (예: `TEST_NOT_FOUND`). `message`는 `/api`에 그대로 노출되므로 반드시 고객친화적 문구로 작성한다. `debugMessage`는 서비스에서 던질 때 원인 파악에 필요한 값(id, 상태 등)을 최대한 담는다.
- 모든 예외는 공통 `ErrorResponse(code, message, debugMessage?)`로 변환된다. Spring MVC 프레임워크 예외(타입 미스매치 400, 미존재 경로 404, 미지원 메서드 405 등)는 `ResponseEntityExceptionHandler` 상속으로 **원래 상태코드를 유지**한 채 ErrorResponse로 변환하고, 그 외 처리 못한 예외만 500 + `INTERNAL_ERROR` 워싱 응답. `NoSuchElementException`/`IllegalArgumentException` 같은 표준 예외를 상태코드에 매핑하는 커스텀 핸들러는 두지 않는다 (내부 버그 은폐 방지 — 비즈니스 에러는 반드시 BusinessException으로).
- 배치/컨슈머 등 HTTP 외 소비자는 `BusinessException`을 직접 catch해서 자체 정책으로 처리한다.

```kotlin
// 서비스에서 — 채널 중립
throw BusinessException(ErrorCode.TEST_NOT_FOUND, debugMessage = "Test not found: id=$id")

// ErrorCode enum에 코드 추가 — 상수명이 곧 코드값
TEST_NOT_FOUND("테스트 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
```
- **`*Service`는 같은 계층의 다른 `*Service`를 참조할 수 없다.** 여러 Service가 공유할 로직은 **`service/component`의 invokable 컴포넌트**로 분리해 주입받는다:
  - **1원칙: 1가지 일만 하는 `fun interface`** (`operator fun invoke`) + **nested `Default` 구현체**(`@Component`) — Service/Repository와 동일한 nested 구현체 컨벤션.
  - 한 컴포넌트가 밀접한 메서드 2~3개를 가져야 하면 일반 `interface`로 두되, 동일하게 nested `Default`를 갖는다. 그 이상 커지면 분리 신호.

```kotlin
// service/component/CalculateFee.kt
fun interface CalculateFee {
    operator fun invoke(amount: Long): Long

    @Component
    class Default : CalculateFee {
        override fun invoke(amount: Long): Long = ...
    }
}

// 사용: class Default(private val calculateFee: CalculateFee) { ... calculateFee(1000L) ... }
```

### 인터페이스 규약 (필수)

- **Service는 반드시 interface로 선언**하고, Controller는 인터페이스에만 의존한다. **Service 기본 구현체는 인터페이스 내부 nested class `Default`(`@Service`)** — 별도 `~Impl` 파일 금지.
- **Repository는 Spring Data 인터페이스 하나로 끝낸다** (구현 클래스·위임 래퍼 없음):
  - `interface FooRepository : Repository<Foo, Long>` — **마커 `Repository` 상속 + 필요한 메서드만 선언**한다. Spring Data가 구현을 자동 생성한다.
  - **`JpaRepository` 통상속 금지** (ArchUnit 강제) — `deleteAll()`, 삭제분 포함 `findAll()` 같은 소프트딜리트 우회 메서드가 노출되기 때문. 조회 메서드는 `~DeletedAtIsNull` 파생 쿼리로 삭제분을 제외한다.
  - DB 없이 돌리는 용도는 별도 InMemory 구현이 아니라 **H2 인메모리가 담당**한다.

```kotlin
interface FooService {
    /** 비즈니스 규약이 있는 메서드는 KDoc으로 적는다 (뻔한 건 생략) */
    fun create(command: CreateCommand): Foo

    data class CreateCommand(
        val name: String,
    )

    @Service
    @Transactional(readOnly = true)
    class Default(
        private val fooRepository: FooRepository,
    ) : FooService {
        @Transactional
        override fun create(command: CreateCommand): Foo = fooRepository.save(Foo.create(name = command.name))
    }
}

// Spring Data가 구현 자동 생성 — 마커 상속 + 필요한 메서드만
interface FooRepository : Repository<Foo, Long> {
    fun save(foo: Foo): Foo   // 저장은 항상 도메인 단위

    fun findByIdAndDeletedAtIsNull(id: Long): Foo?   // 소프트딜리트 제외

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<Foo>
}
```

- 구현체 교체(예: JPA 전환) 시 새 클래스를 만들고 `@Primary`를 붙이거나 기존 구현체의 어노테이션을 제거한다.

### 테스트 규약 (필수)

- **요구사항 구현이 끝나면 반드시 포맷 → 린트 → 테스트를 순서대로 돌린다:**
  ```bash
  ./gradlew ktlintFormat ktlintCheck test koverVerify
  ```
- **단위테스트는 Service 레이어 중심으로 작성**한다. Controller, Repository는 굳이 작성하지 않는다.
- **Kover 커버리지: service 패키지 기준 라인 커버리지 70% 이상** (`koverVerify`가 검증, `check` 태스크에 포함됨). 리포트: `./gradlew koverHtmlReport` → `build/reports/kover/html`.
- **테스트 포맷은 JUnit5로 통일**한다 (`@Test` 메서드 스타일). kotest의 Spec 클래스(BehaviorSpec 등)는 사용하지 않는다.
- **Assertion은 kotest** (`shouldBe`, `shouldThrow`), **Mocking은 mockk** (`mockk`, `every`, `verify`)를 사용한다.
- 테스트 대상 Service에는 mockk로 만든 Repository를 생성자 주입한다. 스프링 컨텍스트를 띄우지 않는 순수 단위테스트로 작성한다.

```kotlin
class FooServiceTest {
    private val fooRepository = mockk<FooRepository>()
    private val fooService = FooService.Default(fooRepository)

    @Test
    fun `create - 저장 후 도메인을 반환한다`() {
        every { fooRepository.save("a") } returns Foo(1L, "a")

        val result = fooService.create(FooService.CreateCommand(name = "a"))

        result.name shouldBe "a"
        verify(exactly = 1) { fooRepository.save("a") }
    }
}
```

- 아키텍처 규칙(계층 규약, 인터페이스 규약)은 `ArchitectureTest.kt`(ArchUnit)가 함께 검증한다.

### 도메인 규약 (필수)

- **비즈니스 로직은 엔티티를 rich하게 사용한다** — 상태 변경·불변식 검증·비즈니스 판단은 도메인 엔티티의 메서드로 둔다. Service는 오케스트레이션(조회 → 도메인 메서드 호출 → 저장)과 트랜잭션 경계만 담당하고, 도메인의 상태를 밖에서 직접 조작하지 않는다 (setter/copy로 상태 바꾸기 금지).
- **Aggregate Root 패턴을 차용한다**:
  - 함께 변경되는 엔티티 묶음(애그리거트)은 루트 엔티티를 통해서만 접근·변경한다 (예: `Order`가 루트면 `OrderLine` 추가/제거는 `order.addLine(...)`으로만).
  - 루트의 컬렉션 연관은 **`cascade = [CascadeType.ALL], orphanRemoval = true`**로 매핑해 루트 `save()`만으로 애그리거트 전체가 영속되게 한다.
  - **Repository는 aggregate root 단위로만 만든다** (`OrderRepository`는 있어도 `OrderLineRepository`는 만들지 않는다). 조회·저장도 루트 단위.
  - 애그리거트 간 참조는 객체 참조 대신 **id 참조**로 한다.
- 불변식이 깨지는 호출은 도메인 메서드 안에서 `BusinessException`을 던진다 (도메인이 스스로 정합성을 지킨다).
- **엔티티 규칙 (필수)**:
  - **도메인 = JPA 엔티티 겸용**이며 **절대 `data class`로 선언하지 않는다** (equals/hashCode/copy가 JPA 프록시·가변 상태와 충돌). 일반 class + `var` + `protected set`.
  - **모든 엔티티는 `domain/BaseEntity`(abstract, `@MappedSuperclass`)를 상속**한다. 공통 필드: `id`(PK, 기본값 `NEW_ID(0L)`, IDENTITY 오토인크리먼트), `createdAt`/`updatedAt`(JPA Auditing 자동 — `config/JpaConfig`의 `@EnableJpaAuditing`), `deletedAt`(소프트딜리트).
  - 중복 처리 가드가 필요한 엔티티는 `domain/BaseIdempotencyEntity`(BaseEntity 상속 + `idempotencyKey` 유니크 컬럼)를 상속한다 (API 설계 규약 7 참고).
  - **id 발급 관례**: 신규 도메인은 `create()` 팩토리(id 미지정 → `NEW_ID`)로 만들고 저장 시 발급된다. id를 밖에서 지정하지 않는다 (테스트 픽스처 제외).
  - **동시 갱신 경합 관례** (재고 차감·잔액 변경 등): 멱등 가드는 "같은 요청의 중복"만 막고 **서로 다른 요청의 동시 갱신(lost update)은 막지 못한다.** 경합 요구가 오면 기본은 **비관락** — repository에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 파생 조회를 추가하고 "락 조회 → 도메인 메서드 → 저장"을 한 트랜잭션에서 수행한다. 경합이 드물면 해당 엔티티에 `@Version` 낙관락 + 충돌 시 409 변환을 대안으로 쓴다.
  - **소프트딜리트 관례**: 도메인의 `delete()`가 불변식 검사 후 BaseEntity의 `markDeleted()`를 호출한다. repository 조회 메서드는 삭제분을 제외한다 (`findByIdAndDeletedAtIsNull` 등).

```kotlin
class Order(
    val id: Long,
    val lines: MutableList<OrderLine>,
    var status: OrderStatus,
) {
    fun cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw BusinessException(ErrorCode.ORDER_ALREADY_SHIPPED, debugMessage = "id=$id, status=$status")
        }
        status = OrderStatus.CANCELED
    }
}

// Service는 오케스트레이션만
override fun cancel(command: CancelCommand): Order {
    val order = orderRepository.findById(command.orderId) ?: throw ...
    order.cancel()
    return orderRepository.save(order)
}
```

### DTO 규약 (필수)

- **Request/Response는 API(핸들러)별 상위 `object`로 묶는다.** **object 명은 원칙적으로 컨트롤러 핸들러 메서드명과 동일**하게 한다 (`createFoo` ↔ `CreateFoo`). 도메인별로 `FooDto.kt` 파일 하나에 해당 도메인의 object들을 모아둔다. 단, **object가 하나뿐인 파일은 object명을 파일명으로** 한다 (ktlint filename 규칙 — 예: `GetHealth.kt`).
- **내부 nested data class 명은 항상 `Request` / `Response`로 고정**한다 (`CreateRequest` 같은 접두 명명 금지). 사용처에서는 `CreateFoo.Request`처럼 object 경로로 구분된다.
- 예외: 여러 도메인이 공유하는 **공통 Request/Response는 object 없이 최상위**에 둔다. 현재 제공: `ErrorResponse`, `PageResponse<T>`(전체 건수 포함 페이징), `SliceResponse<T>`(무한스크롤 — `size+1`개 조회 후 `of()`에 넘기면 hasNext 판단·잘라내기). 목록 API는 요구사항에 맞게 둘 중 하나로 감싼다.
- **모든 필드에 `@field:Schema`로 `description`과 `nullable`을 반드시 명시**한다. Kotlin 타입의 `?` 여부와 `nullable` 값을 일치시킨다.
- **클래스 레벨 `@Schema`에는 `name = "object명+클래스명"`을 명시**한다 (예: `name = "CreateFooRequest"`). nested class 단순명(`Response`)은 스키마 이름 충돌을 일으키므로 필수.
- 도메인 → Response 변환은 Response의 `companion object`에 `from(domain)` 팩토리로, Request → Command 변환은 Request의 `toCommand()` 메서드로 둔다.
- **DTO에는 변환 메서드(`from`/`of`/`toCommand`) 외에 어떤 비즈니스 로직도 넣지 않는다.** 계산·판단·검증 로직은 service(또는 component)의 몫. 단, 단순 포맷팅 변환(날짜 `toDateString()`, 금액 콤마 표기, 마스킹 등 표현 가공)은 DTO에 넣어도 된다.
- API마다 Response를 따로 두는 것이 원칙 (중복 감수 — API별 응답 스펙이 독립적으로 진화할 수 있게).

```kotlin
// controller: fun createFoo(@RequestBody request: CreateFoo.Request): CreateFoo.Response

object CreateFoo {
    @Schema(name = "CreateFooRequest", description = "Foo 생성 요청")
    data class Request(
        @field:Schema(description = "이름", nullable = false)
        val name: String,
        @field:Schema(description = "메모", nullable = true)
        val memo: String?,
    ) {
        fun toCommand(): FooService.CreateCommand = FooService.CreateCommand(name = name)
    }

    @Schema(name = "CreateFooResponse", description = "Foo 생성 응답")
    data class Response(
        @field:Schema(description = "ID", nullable = false)
        val id: Long,
    ) {
        companion object {
            fun from(foo: Foo): Response = Response(id = foo.id)
        }
    }
}
```

### 로깅 규약 (필수)

- 로그 메시지는 문자열 `+` 연결이나 slf4j `{}` 플레이스홀더 대신 **Kotlin `$` 템플릿**으로 쓴다 (길어져도 OK).
- 값은 항상 **`key=$value` named 형식**으로 넣는다. 예: `log.info("샘플 생성: id=${saved.id}, name=${saved.name}")`
- **주요 비즈니스 분기점(생성/상태 전이/삭제/중요 분기 결정)에는 반드시 `info` 로그**를 넣는다.
- 예외 로깅은 `GlobalExceptionHandler`가 일괄 담당한다 — 서비스에서 잡아서 또 찍지 않는다 (중복 로깅 금지).

### 더미(dummy) 규약 (필수)

- `dummy` 패키지는 **local/test phase에서 Swagger 호출 테스트를 쉽게 하기 위한 더미데이터 생성 기능** 전용이다.
- **신규 요건을 구현하면 단위테스트와 함께 `<도메인>ServiceDummyGenerator`(@Component)도 만든다** — 실제 Service 로직을 호출해 더미를 생성한다 (레퍼런스: `SampleServiceDummyGenerator`).
- 더미 API는 `${ApiPath.DUMMY}` prefix의 `DummyController`에 추가한다.
- 생성기·컨트롤러에는 **`@DummyOnly` 하나만 붙이면 된다** — 이 어노테이션에 3중 안전장치가 묶여 있다: ① 메타 `@Profile("local | test")` — live에선 빈 자체가 안 뜬다, ② `DummyOnlyAspect`(AOP, 항상 등록)가 local/test가 아니면 `DUMMY_NOT_ALLOWED`(403)로 차단, ③ dummy 패키지는 본 코드 어디서도 참조 금지 (ArchUnit 강제).

### 설정/프로파일 규약

- phase는 **local / test / live** 3개. 기본값 local (`spring.profiles.default=local`).
  - **local**: 개발 머신. **test**: 배포 검증계(스테이징) — **JUnit 실행 환경이 아니다**. **live**: 운영.
  - JUnit 테스트(@SpringBootTest)는 프로파일을 지정하지 않는다 → 기본 local로 뜬다.
- **JPA**: `spring.jpa.open-in-view=false` 공통. ddl-auto는 local/test `create-drop`, live `validate` (운영 스키마는 `resources/ddl/schema.sql` 기록 기준 수동 반영). DB는 local/test H2(MySQL 모드), live MySQL(드라이버는 운영 확정 시 추가).
- `application.properties`: 모든 phase 공통 설정만 둔다.
- `application-{phase}.properties`: phase별 설정 (예: 로깅 레벨, live는 Swagger 비활성화).
- `application-db.properties`: **데이터소스 연결정보 전용**. `#---` multi-document로 phase별 구분 (`spring.config.activate.on-profile=...`). 각 phase는 `spring.profiles.group.{phase}=db`로 이 파일을 함께 로드한다.
- 실행 예: `./gradlew bootRun --args='--spring.profiles.active=live'`
- **logback**: `logback-spring.xml` 하나로 관리. 공통 root는 **stdout(CONSOLE)** — 컨테이너(Docker/K8s) 환경에서 노드 수집기(FluentBit 등)가 stdout을 수집하는 표준 구조. **콘솔만 찍는 건 local뿐이고, test/live(배포 환경)는 동일 구조**로 파일(VM 배포)·Kafka(AsyncAppender)·Logstash 등 추가 수집 경로를 `test | live` 블록의 주석 처리된 appender 활성화 + `<root>`에 ref 누적 추가로 붙인다. 앱 패키지 로그 레벨은 `application-{phase}.properties`의 `logging.level.*`로 조절.

### DDL 규약

- DB 테이블 생성/변경이 생기면 **`src/main/resources/ddl/schema.sql`에 반드시 누적 기록**한다.
- 날짜 + 변경 내용 주석 후 DDL을 추가하고, 기존 기록은 수정하지 않는다 (append-only).

### 기타 규약

- **Bean Validation(`@Valid`, `spring-boot-starter-validation`)은 사용하지 않는다** — 요청값 검증은 `service/component`(예: `ValidateName`), 상태 전이 불변식은 도메인 메서드가 담당한다. AI가 validation 의존성을 임의 추가하지 말 것.
- **다중 파라미터 함수/생성자 호출에는 named argument를 사용**한다 (예: `Sample.create(name = ..., memo = ...)`).
- **날짜/시간은 항상 `LocalDate` / `LocalDateTime`으로 다룬다** (`Date`, `Calendar`, 날짜 문자열 필드 금지). 표시 표준 포맷은 **`yyyy-MM-dd`** / `yyyy-MM-dd HH:mm:ss` — `utils/extensions/LocalDateExtension.kt`의 `toDateString()`/`toDateTimeString()`을 사용한다.
- 외부 연동 등에서 **`yyyyMMdd` 포맷이 필요하면 String 대신 `domain/vo/BasicDate` VO**(value class, ISO-8601 basic format)를 사용한다. `BasicDate.from(localDate)` / `basicDate.toLocalDate()`로 변환. **공용 VO는 `domain/vo`에 둔다** (domain 무의존 규칙을 그대로 물려받아 모든 계층에서 참조 가능).
- **공용 유틸은 확장함수로 커스텀 정의**해 `utils/extensions`에 둔다. 파일명은 수신 타입 기준 `XxxExtension.kt` (예: `StringExtension.kt`, `LocalDateExtension.kt`). 새 확장함수는 기존 파일에 추가하고, 새 수신 타입이면 파일을 새로 만든다. 유틸 클래스(`XxxUtils.kt`의 object/static성 함수)는 만들지 않는다.
- Controller에는 springdoc 어노테이션(`@Tag`, `@Operation`)을 붙인다.
- 의존성 버전은 `gradle.properties`에서 일괄 관리한다.

## 구현 워크플로우 (필수)

요구사항 구현 시 반드시 아래 순서를 따른다:

1. **테크스펙 작성** — 구현 전에 `.claude/tech-spec/<날짜>-<기능명>.md`에 요구사항·API 설계·도메인 설계·엣지 케이스를 정리한다 (템플릿: `.claude/tech-spec/README.md`). 이후 구현·코드리뷰·PR은 모두 이 문서 기준.
2. **구현** — 위 규약 준수. Sample 도메인 패턴을 레퍼런스로 복제한다. **단위테스트와 `<도메인>ServiceDummyGenerator` + 더미 API까지가 구현 범위**다.
3. **코드리뷰 루프** — `.claude/skills/code-review/SKILL.md` 스킬을 **서브에이전트로 실행**한다. 리뷰 결과의 **P1이 0건이 될 때까지 수정 → 재리뷰를 반복**한다. `DECISION NEEDED`가 나오면 루프를 멈추고 사용자에게 물어본다.
4. **검증** — `./gradlew ktlintFormat ktlintCheck test koverVerify` 실행, 전부 통과 확인.
5. **보고** — 구현 내용, 리뷰 반복 횟수와 반영 사항, 검증 결과를 보고한다.
6. **스테이징은 즉시, 커밋/푸시는 사용자 트리거만.** 작업(검증 포함)이 끝나면 **반드시 `git add -A`로 스테이징까지 해두고** 보고·대기한다 (unstaged 누락 방지 — 신규 파일이 커밋에서 빠지는 사고 차단). 커밋/푸시는 절대 임의로 하지 않는다 — 사용자가 커밋을 요청하면 요구사항 단위로 `<type>: <요구사항>` 커밋(타입: feat/fix/refactor/chore), 푸시·PR을 요청하면 `/commit-pr` 스킬로 수행한다 (PR body에 tech-spec 링크 + 요약).

### AI 실행 규칙 (모델/서브에이전트)

- **병렬화 가능한 코드 수정**(서로 의존성 없는 파일·도메인 단위 작업)은 **sonnet 모델 서브에이전트**를 띄워 분산 실행한다. 순서 의존이 있는 작업은 병렬화하지 않는다. 같은 파일을 두 에이전트가 동시에 수정하지 않도록 파일 단위로 스코프를 나눈다.
- **코드리뷰는 중요하므로 opus 모델 서브에이전트**로 실행한다.
- **모든 서브에이전트 프롬프트에 토큰 최적화 지시를 포함한다**:
  - 핵심만 간결히 — 과정 설명·중간 요약·인사말 생략, 최종 텍스트는 결과만
  - 필요한 파일의 필요한 부분만 읽기 (전체 파일 무차별 재독 금지, 이미 프롬프트로 전달된 내용 재탐색 금지)
  - 코드 전체를 되풀이하지 말고 변경분만 보고

## 빌드/실행

```bash
./gradlew build          # 빌드
./gradlew bootRun        # 실행 → http://localhost:8080/swagger
```

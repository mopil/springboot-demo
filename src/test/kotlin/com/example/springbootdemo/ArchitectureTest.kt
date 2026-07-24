package com.example.springbootdemo

import com.example.springbootdemo.config.ApiPath
import com.example.springbootdemo.domain.BaseEntity
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val BASE = "com.example.springbootdemo"

@AnalyzeClasses(
    packages = [BASE],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {
    // 참조 방향: controller > service > repository (간단한 경우 controller > repository 허용)

    @ArchTest
    val repositoryMustNotDependOnUpperLayers: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE.repository..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("$BASE.controller..", "$BASE.service..")
            .because("repository는 상위 계층(controller, service)을 참조할 수 없다")

    @ArchTest
    val serviceMustNotDependOnControllerLayer: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE.service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("$BASE.controller..")
            .because("service는 controller 계층(controller, controller.dto)을 참조할 수 없다. 입력은 Command, 반환은 도메인/전용 vo로 한다")

    @ArchTest
    val domainMustNotDependOnAnyLayer: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("$BASE.controller..", "$BASE.service..", "$BASE.repository..")
            .because("domain은 어떤 계층에도 의존하지 않는다")

    @ArchTest
    val nothingDependsOnController: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("$BASE.controller..")
            .and()
            .resideOutsideOfPackage("$BASE.config.swagger..")
            .and()
            .resideOutsideOfPackage("$BASE.exception..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$BASE.controller..")
            .because(
                "controller는 최상위 계층이므로 어디서도 참조할 수 없다. 단 " +
                    "config.swagger(Swagger 문서화 지원 코드)와 exception(공통 예외를 ErrorResponse로 변환하는 GlobalExceptionHandler)은 " +
                    "controller.dto(ErrorResponse) 참조를 예외로 허용한다",
            )

    // *Service는 같은 계층의 다른 *Service를 참조할 수 없다 (공통 로직은 invokable 컴포넌트로 분리)

    @ArchTest
    val serviceMustNotDependOnOtherService: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE.service..")
            .should(dependOnAnotherService())
            .because("*Service는 다른 *Service를 참조할 수 없다. 공통 로직은 invokable 컴포넌트로 분리한다")

    // Service / Repository는 interface로 선언한다

    @ArchTest
    val servicesMustBeInterfaces: ArchRule =
        classes()
            .that()
            .resideInAPackage("$BASE.service..")
            .and()
            .haveSimpleNameEndingWith("Service")
            .should()
            .beInterfaces()

    @ArchTest
    val repositoriesMustBeInterfaces: ArchRule =
        classes()
            .that()
            .resideInAPackage("$BASE.repository..")
            .and()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .beInterfaces()

    // 구현체는 별도 ~Impl 파일이 아니라 인터페이스 내부 nested class여야 한다

    @ArchTest
    val implementationsMustBeNestedInsideTheirInterface: ArchRule =
        classes()
            .that()
            .resideInAnyPackage("$BASE.service..", "$BASE.repository..")
            .and()
            .areNotInterfaces()
            .and(implementLayerInterface())
            .should(beNestedInsideImplementedInterface())
            .because("Service/Repository 구현체는 인터페이스 내부 nested class로 둔다 (~Impl 파일 금지)")

    // controller는 구현체가 아니라 인터페이스에만 의존한다

    @ArchTest
    val controllerMustDependOnInterfacesOnly: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("$BASE.controller..")
            .should()
            .dependOnClassesThat(
                object : DescribedPredicate<JavaClass>("*Service/*Repository 구현 클래스") {
                    override fun test(input: JavaClass): Boolean = !input.isInterface && input.allRawInterfaces.any { isLayerInterface(it) }
                },
            ).because("controller는 Service/Repository 인터페이스에만 의존한다 (구현체 직접 주입 금지)")

    // dummy 패키지는 어디서도 참조할 수 없다 (local/test 전용 기능이 본 코드에 새어들지 않도록)

    @ArchTest
    val nothingDependsOnDummy: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("$BASE.dummy..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("$BASE.dummy..")
            .because("dummy는 local/test 전용 — 본 코드(controller/service/repository)가 참조하면 안 된다")

    // Spring Data JPA 보조 인터페이스는 repository 계층 밖에서 직접 사용할 수 없다

    @ArchTest
    val onlyRepositoryLayerMayUseSpringDataJpa: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("$BASE.repository..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(JpaRepository::class.java)
            .because("Spring Data 보조 인터페이스(*JpaRepository)는 도메인 Repository 인터페이스 뒤에 숨긴다")

    // repository 패키지 안에서도 CrudRepository 계열(JpaRepository 포함) 통상속 금지 — 마커 Repository만 허용

    @ArchTest
    val repositoriesMustNotExtendSpringDataCrudInterfaces: ArchRule =
        classes()
            .that()
            .resideInAPackage("$BASE.repository..")
            .should()
            .notBeAssignableTo(CrudRepository::class.java)
            .andShould()
            .notBeAssignableTo(PagingAndSortingRepository::class.java)
            .because("Repository는 마커 Repository 상속 + 필요한 메서드만 선언한다 — 통상속은 소프트딜리트 우회 메서드(deleteAll, 삭제분 포함 findAll)를 노출한다")

    // 엔티티는 BaseEntity를 상속해야 하고 data class로 선언할 수 없다

    @ArchTest
    val entitiesMustExtendBaseEntityAndNotBeDataClass: ArchRule =
        classes()
            .that()
            .areAnnotatedWith(Entity::class.java)
            .should()
            .beAssignableTo(BaseEntity::class.java)
            .andShould(notBeDataClass())
            .because("모든 엔티티는 BaseEntity 상속 + 일반 class (data class는 equals/hashCode/copy가 JPA와 충돌)")

    private fun notBeDataClass(): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>("data class가 아니어야 한다") {
            override fun check(
                item: JavaClass,
                events: ConditionEvents,
            ) {
                val isDataClass = item.methods.any { it.name == "component1" }
                if (isDataClass) {
                    events.add(SimpleConditionEvent.violated(item, "${item.fullName}이 data class로 선언된 엔티티다"))
                } else {
                    events.add(SimpleConditionEvent.satisfied(item, "${item.fullName} OK"))
                }
            }
        }

    // 핸들러는 항상 JSON 객체를 반환한다 (최상위 컬렉션/nullable/빈 응답 금지)

    @ArchTest
    val handlersMustReturnJsonObject: ArchRule =
        methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController::class.java)
            .and()
            .areMetaAnnotatedWith(RequestMapping::class.java)
            .should(returnJsonObjectResponse())
            .because("응답은 항상 JSON 객체 — 최상위 배열/Map, nullable, 빈 응답(Unit) 금지")

    private fun returnJsonObjectResponse(): ArchCondition<JavaMethod> =
        object : ArchCondition<JavaMethod>("JSON 객체(Response)를 반환한다") {
            override fun check(
                item: JavaMethod,
                events: ConditionEvents,
            ) {
                val returnType = item.rawReturnType
                val problem =
                    when {
                        returnType.isAssignableTo(Collection::class.java) || returnType.isAssignableTo(Map::class.java) ->
                            "최상위 컬렉션/Map을 반환한다"
                        returnType.isAssignableTo(ResponseEntity::class.java) ->
                            "ResponseEntity를 반환한다 (Response DTO 직접 반환 + @ResponseStatus 사용)"
                        returnType.name == "void" -> "빈 응답(Unit)을 반환한다"
                        item.isAnnotatedWith("org.jetbrains.annotations.Nullable") -> "nullable을 반환한다"
                        else -> null
                    }
                if (problem == null) {
                    events.add(SimpleConditionEvent.satisfied(item, "${item.fullName} OK"))
                } else {
                    events.add(SimpleConditionEvent.violated(item, "${item.fullName}이 $problem"))
                }
            }
        }

    // 모든 컨트롤러 매핑은 ApiPath prefix(/api, /internal, /admin)로 시작해야 한다

    @ArchTest
    val controllersMustUseApiPathPrefix: ArchRule =
        classes()
            .that()
            .areAnnotatedWith(RestController::class.java)
            .should(haveRequestMappingStartingWithApiPathPrefix())
            .because("모든 컨트롤러는 ApiPath prefix(/api, /internal, /admin)로 시작하는 @RequestMapping을 가져야 한다")

    private fun haveRequestMappingStartingWithApiPathPrefix(): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>("ApiPath prefix로 시작하는 @RequestMapping을 가진다") {
            private val prefixes = listOf(ApiPath.API, ApiPath.INTERNAL, ApiPath.ADMIN, ApiPath.DUMMY)

            override fun check(
                item: JavaClass,
                events: ConditionEvents,
            ) {
                val mapping = item.tryGetAnnotationOfType(RequestMapping::class.java)
                val paths = if (mapping.isPresent) mapping.get().value + mapping.get().path else emptyArray()
                val ok = paths.isNotEmpty() && paths.all { path -> prefixes.any { path == it || path.startsWith("$it/") } }
                if (ok) {
                    events.add(SimpleConditionEvent.satisfied(item, "${item.fullName} OK"))
                } else {
                    events.add(
                        SimpleConditionEvent.violated(
                            item,
                            "${item.fullName}의 @RequestMapping(${paths.joinToString()})이 ApiPath prefix로 시작하지 않는다",
                        ),
                    )
                }
            }
        }

    private fun isLayerInterface(clazz: JavaClass): Boolean =
        clazz.simpleName.endsWith("Service") || clazz.simpleName.endsWith("Repository")

    private fun implementLayerInterface(): DescribedPredicate<JavaClass> =
        object : DescribedPredicate<JavaClass>("*Service/*Repository 인터페이스를 구현한다") {
            override fun test(input: JavaClass): Boolean = input.allRawInterfaces.any { isLayerInterface(it) }
        }

    private fun beNestedInsideImplementedInterface(): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>("자신이 구현한 인터페이스의 nested class여야 한다") {
            override fun check(
                item: JavaClass,
                events: ConditionEvents,
            ) {
                val enclosing = item.enclosingClass
                val ok = enclosing.isPresent && item.allRawInterfaces.contains(enclosing.get())
                if (ok) {
                    events.add(SimpleConditionEvent.satisfied(item, "${item.fullName} OK"))
                } else {
                    events.add(SimpleConditionEvent.violated(item, "${item.fullName}이 인터페이스 외부에 정의된 구현체다"))
                }
            }
        }

    private fun dependOnAnotherService(): ArchCondition<JavaClass> =
        object : ArchCondition<JavaClass>("다른 *Service를 참조한다") {
            override fun check(
                item: JavaClass,
                events: ConditionEvents,
            ) {
                val originTop = topLevelOf(item)
                item.directDependenciesFromSelf.forEach { dependency ->
                    val targetTop = topLevelOf(dependency.targetClass)
                    if (targetTop.simpleName.endsWith("Service") &&
                        targetTop.packageName.startsWith("$BASE.service") &&
                        targetTop.fullName != originTop.fullName
                    ) {
                        events.add(SimpleConditionEvent.satisfied(item, dependency.description))
                    }
                }
            }
        }

    private fun topLevelOf(clazz: JavaClass): JavaClass {
        var current = clazz
        while (current.enclosingClass.isPresent) {
            current = current.enclosingClass.get()
        }
        return current
    }
}

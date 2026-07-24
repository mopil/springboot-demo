pluginManagement {
    val kotlinVersion: String by settings
    val springBootVersion: String by settings
    val dependencyManagementVersion: String by settings
    val koverVersion: String by settings
    val ktlintPluginVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        kotlin("plugin.jpa") version kotlinVersion
        id("org.springframework.boot") version springBootVersion
        id("io.spring.dependency-management") version dependencyManagementVersion
        id("org.jetbrains.kotlinx.kover") version koverVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintPluginVersion
    }
}

rootProject.name = "springboot-demo"

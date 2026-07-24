package com.example.springbootdemo.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/** BaseEntity의 createdAt/updatedAt 자동 관리 (JPA Auditing) */
@Configuration
@EnableJpaAuditing
class JpaConfig

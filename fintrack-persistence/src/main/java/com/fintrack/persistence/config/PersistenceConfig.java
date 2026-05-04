package com.fintrack.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.fintrack.persistence.entity")
@EnableJpaRepositories(basePackages = "com.fintrack.persistence.repository")
public class PersistenceConfig {
}

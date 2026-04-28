package com.webjpa.shopping.config;

import com.webjpa.shopping.repository.MemberRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackageClasses = MemberRepository.class)
public class JpaRepositoryConfig {
}

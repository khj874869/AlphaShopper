package com.webjpa.shopping.config;

import com.webjpa.shopping.search.ProductSearchRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@ConditionalOnProperty(name = "app.search.enabled", havingValue = "true", matchIfMissing = true)
@EnableElasticsearchRepositories(basePackageClasses = ProductSearchRepository.class)
public class ElasticsearchRepositoryConfig {
}

package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.repository.ProductRepository;
import com.webjpa.shopping.search.ProductDocument;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductSearchIndexService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchIndexService.class);

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final boolean reindexOnStartup;

    public ProductSearchIndexService(ProductRepository productRepository,
                                     ProductSearchRepository productSearchRepository,
                                     ElasticsearchOperations elasticsearchOperations,
                                     @Value("${app.search.reindex-on-startup:true}") boolean reindexOnStartup) {
        this.productRepository = productRepository;
        this.productSearchRepository = productSearchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.reindexOnStartup = reindexOnStartup;
    }

    public void index(Product product) {
        ensureIndex();
        productSearchRepository.save(ProductDocument.from(product));
    }

    public long reindexAll() {
        ensureIndex();
        List<ProductDocument> documents = productRepository.findAll().stream()
                .map(ProductDocument::from)
                .toList();

        productSearchRepository.deleteAll();
        productSearchRepository.saveAll(documents);
        return documents.size();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void reindexAtStartup() {
        if (!reindexOnStartup) {
            return;
        }

        try {
            long count = reindexAll();
            log.info("Elasticsearch product index synchronized. count={}", count);
        } catch (Exception ex) {
            log.warn("Failed to synchronize Elasticsearch product index at startup: {}", ex.getMessage());
        }
    }

    private void ensureIndex() {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOperations.exists()) {
            return;
        }

        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping());
    }
}

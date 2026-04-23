package com.webjpa.shopping.service;

import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.logging.LogValues;
import com.webjpa.shopping.repository.ProductRepository;
import com.webjpa.shopping.search.ProductDocument;
import com.webjpa.shopping.search.ProductSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider;
    private final ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider;
    private final boolean searchEnabled;
    private final boolean reindexOnStartup;
    private final boolean recreateIndexOnReindex;

    public ProductSearchIndexService(ProductRepository productRepository,
                                     ObjectProvider<ProductSearchRepository> productSearchRepositoryProvider,
                                     ObjectProvider<ElasticsearchOperations> elasticsearchOperationsProvider,
                                     @Value("${app.search.enabled:true}") boolean searchEnabled,
                                     @Value("${app.search.reindex-on-startup:true}") boolean reindexOnStartup,
                                     @Value("${app.search.recreate-index-on-reindex:false}") boolean recreateIndexOnReindex) {
        this.productRepository = productRepository;
        this.productSearchRepositoryProvider = productSearchRepositoryProvider;
        this.elasticsearchOperationsProvider = elasticsearchOperationsProvider;
        this.searchEnabled = searchEnabled;
        this.reindexOnStartup = reindexOnStartup;
        this.recreateIndexOnReindex = recreateIndexOnReindex;
    }

    public void index(Product product) {
        if (!searchEnabled) {
            log.debug("event=product_search.index.skipped reason=search_disabled productId={}", product.getId());
            return;
        }

        ProductSearchRepository productSearchRepository = productSearchRepositoryProvider.getIfAvailable();
        if (productSearchRepository == null) {
            log.debug("event=product_search.index.skipped reason=repository_unavailable productId={}", product.getId());
            return;
        }

        ensureIndex(false);
        productSearchRepository.save(ProductDocument.from(product));
        log.debug("event=product_search.index.saved productId={} active={}", product.getId(), product.isActive());
    }

    public long reindexAll() {
        if (!searchEnabled) {
            log.info("event=product_search.reindex.skipped reason=search_disabled");
            return 0L;
        }

        ProductSearchRepository productSearchRepository = productSearchRepositoryProvider.getIfAvailable();
        if (productSearchRepository == null) {
            log.warn("event=product_search.reindex.skipped reason=repository_unavailable");
            return 0L;
        }

        ensureIndex(recreateIndexOnReindex);
        List<ProductDocument> documents = productRepository.findAll().stream()
                .map(ProductDocument::from)
                .toList();

        productSearchRepository.deleteAll();
        productSearchRepository.saveAll(documents);
        log.info("event=product_search.reindex.completed count={}", documents.size());
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
            log.info("event=product_search.startup_reindex.completed count={}", count);
        } catch (Exception ex) {
            log.warn("event=product_search.startup_reindex.failed errorType={} error={}",
                    ex.getClass().getSimpleName(),
                    LogValues.safe(ex.getMessage()),
                    ex);
        }
    }

    private void ensureIndex(boolean recreateExistingIndex) {
        ElasticsearchOperations elasticsearchOperations = elasticsearchOperationsProvider.getIfAvailable();
        if (elasticsearchOperations == null) {
            return;
        }

        IndexOperations indexOperations = elasticsearchOperations.indexOps(ProductDocument.class);
        if (indexOperations.exists() && !recreateExistingIndex) {
            return;
        }
        if (indexOperations.exists()) {
            indexOperations.delete();
            log.info("event=product_search.index.deleted documentType={}", ProductDocument.class.getSimpleName());
        }

        indexOperations.create();
        indexOperations.putMapping(indexOperations.createMapping());
        log.info("event=product_search.index.created documentType={}", ProductDocument.class.getSimpleName());
    }
}

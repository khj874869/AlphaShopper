package com.webjpa.shopping.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    Page<ProductDocument> findByActiveTrue(Pageable pageable);

    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["name^3", "brand^2", "description"]
                    }
                  }
                ],
                "filter": [
                  {
                    "term": {
                      "active": true
                    }
                  }
                ]
              }
            }
            """)
    Page<ProductDocument> searchByKeyword(String keyword, Pageable pageable);
}


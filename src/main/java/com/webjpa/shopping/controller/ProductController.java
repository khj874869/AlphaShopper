package com.webjpa.shopping.controller;

import com.webjpa.shopping.dto.CreateProductRequest;
import com.webjpa.shopping.dto.ProductSearchPageResponse;
import com.webjpa.shopping.dto.ProductResponse;
import com.webjpa.shopping.service.ProductSearchIndexService;
import com.webjpa.shopping.service.ProductSearchService;
import com.webjpa.shopping.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductSearchService productSearchService;
    private final ProductSearchIndexService productSearchIndexService;

    public ProductController(ProductService productService,
                             ProductSearchService productSearchService,
                             ProductSearchIndexService productSearchIndexService) {
        this.productService = productService;
        this.productSearchService = productSearchService;
        this.productSearchIndexService = productSearchIndexService;
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @GetMapping("/search")
    public ProductSearchPageResponse search(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return productSearchService.search(keyword, page, size);
    }

    @PostMapping("/search/reindex")
    public String reindex() {
        long indexedCount = productSearchIndexService.reindexAll();
        return "Indexed products: " + indexedCount;
    }
}

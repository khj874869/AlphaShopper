package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.dto.CreateProductRequest;
import com.webjpa.shopping.dto.ProductResponse;
import com.webjpa.shopping.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchIndexService productSearchIndexService;

    public ProductService(ProductRepository productRepository, ProductSearchIndexService productSearchIndexService) {
        this.productRepository = productRepository;
        this.productSearchIndexService = productSearchIndexService;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product(
                request.name(),
                request.brand(),
                request.price(),
                request.stockQuantity(),
                request.description(),
                request.imageUrl()
        );

        Product savedProduct = productRepository.save(product);
        productSearchIndexService.index(savedProduct);
        return ProductResponse.from(savedProduct);
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(ProductResponse::from).toList();
    }

    public ProductResponse get(Long productId) {
        return ProductResponse.from(getEntity(productId));
    }

    public Product getEntity(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found. id=" + productId));
    }
}

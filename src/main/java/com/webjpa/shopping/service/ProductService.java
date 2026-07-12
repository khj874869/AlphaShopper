package com.webjpa.shopping.service;

import com.webjpa.shopping.common.ApiException;
import com.webjpa.shopping.domain.Product;
import com.webjpa.shopping.dto.CreateProductRequest;
import com.webjpa.shopping.dto.ProductResponse;
import com.webjpa.shopping.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public Map<Long, Product> getEntitiesByIds(Collection<Long> productIds) {
        Set<Long> uniqueProductIds = uniqueIds(productIds);
        if (uniqueProductIds.isEmpty()) {
            return Map.of();
        }
        return mapAndValidate(uniqueProductIds, productRepository.findAllById(uniqueProductIds));
    }

    public Map<Long, Product> getEntitiesByIdsForUpdate(Collection<Long> productIds) {
        Set<Long> uniqueProductIds = uniqueIds(productIds);
        if (uniqueProductIds.isEmpty()) {
            return Map.of();
        }
        return mapAndValidate(uniqueProductIds, productRepository.findAllByIdForUpdate(uniqueProductIds));
    }

    private Set<Long> uniqueIds(Collection<Long> productIds) {
        Set<Long> uniqueProductIds = productIds.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return uniqueProductIds;
    }

    private Map<Long, Product> mapAndValidate(Collection<Long> productIds, Collection<Product> products) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Product> productsById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (Long productId : productIds) {
            if (!productsById.containsKey(productId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Product not found. id=" + productId);
            }
        }

        return productsById;
    }
}

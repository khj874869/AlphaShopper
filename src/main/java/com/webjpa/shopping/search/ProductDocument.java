package com.webjpa.shopping.search;

import com.webjpa.shopping.domain.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;

@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/product-settings.json")
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "shopping_index_analyzer", searchAnalyzer = "shopping_search_analyzer")
    private String name;

    @Field(type = FieldType.Text, analyzer = "shopping_index_analyzer", searchAnalyzer = "shopping_search_analyzer")
    private String brand;

    @Field(type = FieldType.Text, analyzer = "shopping_index_analyzer", searchAnalyzer = "shopping_search_analyzer")
    private String description;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private int stockQuantity;

    @Field(type = FieldType.Boolean)
    private boolean active;

    protected ProductDocument() {
    }

    public ProductDocument(Long id, String name, String brand, String description, String imageUrl, BigDecimal price,
                           int stockQuantity, boolean active) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = active;
    }

    public static ProductDocument from(Product product) {
        return new ProductDocument(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.getImageUrl(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public boolean isActive() {
        return active;
    }
}

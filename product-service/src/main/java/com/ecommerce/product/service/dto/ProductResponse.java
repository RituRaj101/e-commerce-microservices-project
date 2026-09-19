package com.ecommerce.product.service.dto;

import com.ecommerce.product.service.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * This is the object that actually gets stored in Redis (as JSON - see
 * RedisConfig). It needs a no-arg constructor so Jackson can deserialize it
 * back out of Redis into a Java object.
 *
 * Interview point: we cache the DTO, not the Entity. Caching the Entity
 * directly is a common mistake - Entities are proxied by Hibernate (lazy
 * loading etc.) and don't serialize cleanly to JSON. DTOs are plain,
 * predictable, and safe to serialize.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory()
        );
    }
}

package com.ecommerce.order.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * This is a DELIBERATE duplicate of Product Service's ProductResponse
 * shape, kept locally in Order Service.
 *
 * Interview point - "why not share one DTO class between services?":
 * true microservices should NOT share code/database schemas between
 * services, because that recreates tight coupling (the exact problem
 * microservices are meant to avoid) - if Product Service's DTO shape
 * changed, Order Service would break unexpectedly through a shared
 * library instead of through an explicit, versioned API contract.
 * Some teams do share a small "API contract" library for this reason,
 * but plain duplication is simpler and perfectly reasonable at this
 * project's scale.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
}

package com.ecommerce.product.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Interview point on using BigDecimal for price instead of double/float:
 * floating-point types (double/float) cannot represent most decimal
 * fractions exactly (0.1 + 0.2 != 0.3 in floating point), which is
 * unacceptable for money. BigDecimal represents decimals exactly and is
 * the standard choice for any currency field in Java.
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(length = 80)
    private String category;
}

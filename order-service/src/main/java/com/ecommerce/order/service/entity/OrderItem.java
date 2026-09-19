package com.ecommerce.order.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Again, just a plain id - Product lives in Product Service's own
    // database, so there's no JPA @ManyToOne to a Product entity here.
    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    // Price PER UNIT at the moment the order was placed - captured here
    // deliberately (not just looked up live later), because product prices
    // can change after an order is placed. An order's total should reflect
    // what the customer actually agreed to pay, not today's price.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtOrderTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}

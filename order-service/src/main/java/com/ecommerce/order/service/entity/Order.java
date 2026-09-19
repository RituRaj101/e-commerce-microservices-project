package com.ecommerce.order.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Interview point on @OneToMany here:
 * - mappedBy = "order": OrderItem is the OWNING side of the relationship
 *   (it holds the foreign key column). Order is the INVERSE side - it
 *   doesn't have its own FK column, it just describes "I have many of
 *   these OrderItems whose order_id points back to me."
 * - cascade = ALL: saving/deleting an Order automatically saves/deletes
 *   its OrderItems too - you never manage OrderItems independently of
 *   their parent Order.
 * - orphanRemoval = true: if an OrderItem is REMOVED from this list in
 *   Java (not just the Order deleted), Hibernate deletes that row from
 *   the DB too - keeps the collection and the table in sync.
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // In a full system this would be a foreign key to User Service's user id.
    // Since services don't share a database, we just store the plain id here,
    // not a JPA relationship to a User entity (that entity lives in a
    // different service/database entirely).
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Convenience method to keep both sides of the bidirectional
    // relationship in sync when adding an item - a common JPA gotcha is
    // setting only one side and ending up with an inconsistent object graph.
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}

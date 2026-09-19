package com.ecommerce.order.service.service.impl;

import com.ecommerce.order.service.client.ProductServiceClient;
import com.ecommerce.order.service.dto.*;
import com.ecommerce.order.service.entity.Order;
import com.ecommerce.order.service.entity.OrderItem;
import com.ecommerce.order.service.entity.OrderStatus;
import com.ecommerce.order.service.exception.InsufficientStockException;
import com.ecommerce.order.service.exception.ResourceNotFoundException;
import com.ecommerce.order.service.exception.ValidationException;
import com.ecommerce.order.service.repository.OrderRepository;
import com.ecommerce.order.service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    // This is a Spring-managed bean even though we never wrote a class
    // implementing it - @EnableFeignClients + @FeignClient generated the
    // implementation, and it's injected here exactly like any other
    // @Service bean would be.
    private final ProductServiceClient productServiceClient;

    @Override
    public OrderResponse placeOrder(OrderRequest request) {
        validateOrderRequest(request);

        Order order = Order.builder()
                .userId(request.getUserId())
                .createdAt(LocalDateTime.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            // THIS is the actual cross-service call: Order Service asks
            // Product Service, live, over HTTP (routed through Eureka),
            // "what is the current stock and price for this product?"
            // Note: if Product Service is down or the id doesn't exist,
            // this line throws a FeignException - handled centrally in
            // GlobalExceptionHandler, not here.
            ProductDto product = productServiceClient.getProductById(itemRequest.getProductId());

            if (product.getStock() < itemRequest.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for product '" + product.getName() +
                                "'. Requested: " + itemRequest.getQuantity() +
                                ", Available: " + product.getStock()
                );
            }

            // Price is taken from Product Service's LIVE response, never
            // from anything the client might have sent - otherwise a
            // client could submit a fake low price in the request body.
            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemRequest.getQuantity())
                    .priceAtOrderTime(product.getPrice())
                    .build();

            order.addItem(orderItem); // keeps both sides of the relationship in sync

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        order.setTotalAmount(total);
        order.setStatus(OrderStatus.CONFIRMED);

        // cascade = ALL on Order.items means saving the Order also saves
        // every OrderItem in one call - no separate repository for items.
        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return OrderResponse.fromEntity(order);
    }

    @Override
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.getUserId() == null) {
            throw new ValidationException("userId is required");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException("Order must contain at least one item");
        }
        for (OrderItemRequest item : request.getItems()) {
            if (item.getProductId() == null) {
                throw new ValidationException("Each item must have a productId");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ValidationException("Quantity must be greater than zero");
            }
        }
    }
}

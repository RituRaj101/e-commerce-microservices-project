package com.ecommerce.order.service.service;

import com.ecommerce.order.service.dto.OrderRequest;
import com.ecommerce.order.service.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(OrderRequest request);
    OrderResponse getOrderById(Long id);
    List<OrderResponse> getOrdersByUser(Long userId);
}

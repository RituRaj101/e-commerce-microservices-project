package com.ecommerce.order.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    // No JWT yet, so userId is passed directly in the body rather than
    // extracted from an auth token - a stand-in until security is added
    // back in a later phase.
    private Long userId;
    private List<OrderItemRequest> items;
}

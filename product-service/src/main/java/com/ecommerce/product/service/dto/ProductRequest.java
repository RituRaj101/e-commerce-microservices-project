package com.ecommerce.product.service.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * No Bean Validation annotations (consistent with the rest of this
 * project's scope) - checks are done manually in ProductServiceImpl.
 */
@Data
public class ProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
}

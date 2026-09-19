package com.ecommerce.product.service.service.impl;

import com.ecommerce.product.service.dto.ProductRequest;
import com.ecommerce.product.service.dto.ProductResponse;
import com.ecommerce.product.service.entity.Product;
import com.ecommerce.product.service.exception.ResourceNotFoundException;
import com.ecommerce.product.service.exception.ValidationException;
import com.ecommerce.product.service.repository.ProductRepository;
import com.ecommerce.product.service.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class implements the CACHE-ASIDE pattern (also called "lazy
 * loading" caching):
 *   - On READ: check Redis first. On a miss, fall through to the method
 *     body (which hits Oracle), and Spring automatically stores the
 *     result back into Redis for next time.
 *   - On WRITE (update/delete): don't try to update the cache in place -
 *     just EVICT the stale entry, so the next read naturally repopulates
 *     it from the database. This is simpler and less error-prone than
 *     trying to keep the cache in sync with every write.
 *
 * All of this is achieved declaratively with annotations - no manual
 * "check Redis, then check DB" if/else code, unlike a hand-rolled
 * RedisTemplate approach.
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    private static final String CACHE_NAME = "products";

    @Override
    // @CachePut ALWAYS runs the method body (unlike @Cacheable, which
    // skips the method on a cache hit) and stores whatever it returns -
    // appropriate here because a newly created product has no cache
    // entry to skip to yet, and we want it cached immediately.
    @CachePut(value = CACHE_NAME, key = "#result.id")
    public ProductResponse createProduct(ProductRequest request) {
        validateProductRequest(request);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .build();

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Override
    // Cache key is the product id. First call for a given id hits Oracle
    // and populates Redis under key "products::<id>"; every call after
    // that (within the 10-minute TTL) returns straight from Redis without
    // this method body executing at all.
    @Cacheable(value = CACHE_NAME, key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Override
    // Deliberately NOT cached: a full catalog listing changes too often
    // relative to how often it's re-read to benefit much from caching,
    // and caching a whole list is harder to keep coherent with per-id
    // cache entries (this is a common real-world caching trade-off to be
    // able to explain in an interview).
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    // @CachePut refreshes the cache entry with the NEW data immediately,
    // rather than evicting and waiting for the next read to repopulate it -
    // appropriate for updates where we already have the fresh data in hand.
    @CachePut(value = CACHE_NAME, key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        validateProductRequest(request);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    @Override
    // @CacheEvict removes the stale entry entirely - there's no "new
    // data" to put back after a delete, so eviction (not @CachePut) is
    // the right tool here.
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private void validateProductRequest(ProductRequest request) {
        if (isBlank(request.getName())) {
            throw new ValidationException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price must be greater than zero");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new ValidationException("Stock cannot be negative");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

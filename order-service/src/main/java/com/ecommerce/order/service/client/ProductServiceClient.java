package com.ecommerce.order.service.client;

import com.ecommerce.order.service.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * This is the entire "HTTP client" for calling Product Service - just an
 * interface, no implementation. At startup, Spring Cloud OpenFeign
 * generates a dynamic proxy implementing this interface, using the
 * @GetMapping path exactly as if this were a @RestController, except it
 * fires an OUTGOING request instead of handling an incoming one.
 *
 * name = "product-service" is the KEY piece: this is not a URL, it's the
 * Eureka service name Product Service registered itself under
 * (spring.application.name in Product Service's properties). At call time,
 * Feign asks Eureka "give me a live instance of product-service" and sends
 * the request there - this is the actual payoff of everything we set up
 * with Eureka Server.
 *
 * If you needed to call this WITHOUT Eureka (e.g. in a test, or if a
 * project doesn't use service discovery), you'd add a url = "http://
 * localhost:8082" attribute instead.
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable("id") Long id);
}

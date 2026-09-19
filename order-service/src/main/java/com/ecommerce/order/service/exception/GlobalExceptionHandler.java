package com.ecommerce.order.service.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidation(ValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Object> handleInsufficientStock(InsufficientStockException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Interview point: when the Feign call to Product Service gets back a
     * 404 (product id doesn't exist there), Feign doesn't return null - it
     * throws a FeignException.NotFound. If we DIDN'T catch this
     * specifically, the client calling Order Service would see a raw
     * 500 Internal Server Error with a confusing Feign stack trace instead
     * of a clean, meaningful response. This handler translates that
     * cross-service failure into a proper 404 for OUR client.
     */
    @ExceptionHandler(FeignException.NotFound.class)
    public ResponseEntity<Object> handleProductNotFound(FeignException.NotFound ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Referenced product does not exist");
    }

    // Catch-all for other Feign failures - e.g. Product Service is down
    // entirely, or Eureka has no live instance to route to.
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignFailure(FeignException ex) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Product Service is currently unavailable, please try again shortly");
    }

    private ResponseEntity<Object> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}

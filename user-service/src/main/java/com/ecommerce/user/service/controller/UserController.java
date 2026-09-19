package com.ecommerce.user.service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.user.service.dto.LoginRequest;
import com.ecommerce.user.service.dto.RegisterRequest;
import com.ecommerce.user.service.dto.UserResponse;
import com.ecommerce.user.service.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * Interview point: the controller has ZERO business logic. It only:
 *   1. Declares the route (@PostMapping, @GetMapping)
 *   2. Validates the incoming shape (@Valid triggers Bean Validation on the DTO)
 *   3. Delegates to the service layer
 *   4. Chooses the HTTP status code for the response
 * If asked "why is your controller so thin?" - that's the answer: keeping
 * HTTP concerns separate from business rules makes both easier to test and
 * change independently.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);
        // 201 CREATED, not 200 OK - a new resource was created.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
        UserResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}

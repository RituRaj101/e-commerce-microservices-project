package com.ecommerce.user.service.service;

import com.ecommerce.user.service.dto.LoginRequest;
import com.ecommerce.user.service.dto.RegisterRequest;
import com.ecommerce.user.service.dto.UserResponse;

public interface UserService {
    UserResponse register(RegisterRequest request);
    UserResponse login(LoginRequest request);
    UserResponse getUserById(Long id);
}

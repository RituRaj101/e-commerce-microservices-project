package com.ecommerce.user.service.dto;

import com.ecommerce.user.service.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * What the SERVER sends back. Deliberately excludes the password field -
 * this is the whole reason DTOs exist instead of returning User directly.
 */
@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;

    // Static factory method: converts Entity -> DTO in one clear place,
    // instead of scattering .setX() calls across the codebase.
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}

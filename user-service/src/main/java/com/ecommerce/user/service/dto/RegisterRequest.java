package com.ecommerce.user.service.dto;

import lombok.Data;

/**
 * What the CLIENT sends when registering.
 * Notice: no "id", no "role" here - role defaults to CUSTOMER in the
 * service layer. We never let the client dictate their own role from the
 * request body - that would let anyone self-promote to ADMIN.
 *
 * No Bean Validation annotations here (validation starter excluded) -
 * required-field/format checks are done manually in UserServiceImpl instead.
 * Trade-off worth mentioning in an interview: annotation-based validation
 * fails fast at the framework level before your code even runs and keeps
 * the check next to the field it validates; manual checks give more
 * control but must be remembered and kept in sync by hand.
 */
@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
}

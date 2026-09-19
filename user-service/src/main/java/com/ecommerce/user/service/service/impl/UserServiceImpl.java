package com.ecommerce.user.service.service.impl;

import com.ecommerce.user.service.dto.LoginRequest;
import com.ecommerce.user.service.dto.RegisterRequest;
import com.ecommerce.user.service.dto.UserResponse;
import com.ecommerce.user.service.entity.User;
import com.ecommerce.user.service.exception.DuplicateEmailException;
import com.ecommerce.user.service.exception.InvalidCredentialsException;
import com.ecommerce.user.service.exception.ResourceNotFoundException;
import com.ecommerce.user.service.exception.ValidationException;
import com.ecommerce.user.service.repository.UserRepository;
import com.ecommerce.user.service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;

    @Override
    public UserResponse register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(hashPassword(request.getPassword()))
                .role(User.Role.CUSTOMER) // role is always set server-side, never client-supplied
                .build();

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }

    @Override
    public UserResponse login(LoginRequest request) {
        if (isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new ValidationException("Email and password are required");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!verifyPassword(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    // ---- manual validation (Bean Validation / @Valid was excluded) ----
    private void validateRegisterRequest(RegisterRequest request) {
        if (isBlank(request.getName())) {
            throw new ValidationException("Name is required");
        }
        if (isBlank(request.getEmail()) || !request.getEmail().contains("@")) {
            throw new ValidationException("A valid email is required");
        }
        if (isBlank(request.getPassword()) || request.getPassword().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Manual SHA-256 hashing with a random per-user salt, since BCrypt
     * (from spring-security-crypto) was excluded from this project's scope.
     *
     * Why we still add a salt even without BCrypt: SHA-256 alone is
     * deterministic - the same password always produces the same hash, so
     * two users with password "test123" would have identical hash values,
     * and precomputed "rainbow table" attacks become feasible. Prepending
     * a random salt before hashing means each user's hash is unique even
     * for identical passwords, and a rainbow table built for the plain
     * SHA-256 of common passwords no longer works directly.
     *
     * Stored format: "<salt_base64>:<hash_base64>" so verifyPassword() can
     * pull the same salt back out for the comparison.
     *
     * Honest caveat worth stating in an interview: SHA-256 is a
     * general-purpose fast hash, not a password-hashing algorithm - unlike
     * BCrypt/Argon2, it has no built-in "work factor" to slow down brute
     * force. This implementation favors zero extra dependencies for the
     * current project phase; BCrypt/Argon2 via Spring Security is the
     * production-grade upgrade path.
     */
    private String hashPassword(String rawPassword) {
        byte[] salt = generateSalt();
        byte[] hash = sha256(salt, rawPassword);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(String rawPassword, String storedValue) {
        String[] parts = storedValue.split(":");
        if (parts.length != 2) {
            return false; // malformed stored value - treat as no match
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = sha256(salt, rawPassword);
        return MessageDigest.isEqual(expectedHash, actualHash); // constant-time compare,
                                                                 // avoids timing attacks
    }

    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private byte[] sha256(byte[] salt, String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            digest.update(salt);
            return digest.digest(rawPassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM - this branch is
            // effectively unreachable, but the checked exception must be handled.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}

package com.ecommerce.user.service.repository;

import com.ecommerce.user.service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Interview point: we never implement this interface ourselves.
 * Spring Data JPA generates the implementation at runtime (a dynamic proxy)
 * by parsing the METHOD NAME. "findByEmail" is parsed into the SQL:
 *   SELECT * FROM users WHERE email = ?
 * JpaRepository<User, Long> already gives us save(), findById(), findAll(),
 * deleteById() etc. for free - we only declare methods for queries that
 * aren't covered by those defaults.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}

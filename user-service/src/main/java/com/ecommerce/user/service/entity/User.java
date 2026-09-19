package com.ecommerce.user.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps to the "users" table.
 *
 * Interview talking points:
 * - @Entity + @Table: tells JPA/Hibernate this class maps to a DB table.
 *   @Table is optional if the table name matches the class name, but being
 *   explicit avoids surprises (Hibernate pluralizes/lowercases differently
 *   across versions/dialects).
 * - @GeneratedValue(strategy = IDENTITY): delegates ID generation to the DB.
 *   Oracle 12c+ (including 21c) supports native IDENTITY columns, so this
 *   still works - but the more idiomatic Oracle approach is SEQUENCE
 *   (strategy = GenerationType.SEQUENCE with @SequenceGenerator), since
 *   Oracle sequences pre-date IDENTITY support and allow Hibernate to batch
 *   inserts more efficiently. IDENTITY is used here to keep the entity
 *   portable across databases without extra config - worth mentioning as a
 *   conscious trade-off if asked in an interview.
 * - unique = true on email: DB-level constraint, not just application-level
 *   validation - prevents race conditions where two requests register the
 *   same email simultaneously.
 */
@Entity
@Table(name = "users")
@Data // Lombok: generates getters/setters/equals/hashCode/toString
@NoArgsConstructor // JPA requires a no-arg constructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Stores the BCrypt HASH, never the plaintext password.
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) // stores "CUSTOMER"/"ADMIN" as readable text, not 0/1
    @Column(nullable = false)
    private Role role;

    public enum Role {
        CUSTOMER, ADMIN
    }
}

package com.treserve.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    // nullable — Google-пользователи не имеют пароля
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    @Builder.Default
    private String role = "USER";

    /** LOCAL = email/пароль, GOOGLE = OAuth2 */
    @Column(name = "auth_provider", length = 20)
    @Builder.Default
    private String authProvider = "LOCAL";

    /** Google sub claim — уникальный ID пользователя у Google */
    @Column(name = "provider_id")
    private String providerId;

    /** URL аватарки из Google профиля */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}

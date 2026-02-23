package com.test.system.model.auth;

import com.test.system.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_tokens", indexes = {
        @Index(name = "idx_api_tokens_prefix", columnList = "tokenPrefix"),
        @Index(name = "idx_api_tokens_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiToken {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 32, unique = true)
    private String tokenPrefix;

    @Column(nullable = false, length = 128)
    private String secretHash;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant lastUsedAt;

    private Instant revokedAt;

    @Column(length = 128)
    private String scopes;

    public boolean isActive() {
        return revokedAt == null;
    }
}

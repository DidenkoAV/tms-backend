package com.test.system.model.user;

import com.test.system.model.group.Group;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * User preferences including current group selection and UI settings.
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_group_id")
    private Group currentGroup;

    @Column(length = 16)
    @Builder.Default
    private String theme = "light";

    @Column(length = 8)
    @Builder.Default
    private String language = "en";

    @Column(length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}


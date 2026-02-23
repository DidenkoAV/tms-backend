package com.test.system.model.suite;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "suites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "is_archived", nullable = false)
    @Builder.Default
    private boolean archived = false;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

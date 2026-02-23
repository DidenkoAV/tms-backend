package com.test.system.model.run;

import com.test.system.model.milestone.Milestone;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Run {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    @Column(name = "created_by")
    private Long createdBy;

    @ManyToMany(mappedBy = "runs")
    @Builder.Default
    private List<Milestone> milestones = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
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

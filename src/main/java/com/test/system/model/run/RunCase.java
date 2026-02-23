package com.test.system.model.run;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "run_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="run_id", nullable = false)
    private Long runId;

    @Column(name="case_id", nullable = false)
    private Long caseId;

    @Column(name="assignee_id")
    private Long assigneeId;

    @Column(name="current_status_id")
    private Long currentStatusId;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name="updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}

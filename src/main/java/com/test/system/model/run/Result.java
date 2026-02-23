package com.test.system.model.run;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Result {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="run_case_id", nullable = false)
    private Long runCaseId;

    @Column(name="status_id", nullable = false)
    private Long statusId;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name="defects_json", columnDefinition = "text")
    private String defectsJson;

    @Column(name="elapsed_seconds")
    private Integer elapsedSeconds;

    @Column(name="created_by")
    private Long createdBy;

    @Column(name="created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

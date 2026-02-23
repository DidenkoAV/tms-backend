package com.test.system.model.jira;

import com.test.system.model.cases.TestCase;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "test_case_issues")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TestCaseIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private TestCase testCase;

    @Column(nullable = false)
    private String issueKey;

    @Column(nullable = false)
    private String issueUrl;

    @Column(name = "status")
    private String status;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

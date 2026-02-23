package com.test.system.model.cases;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "cases")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "suite_id")
    private Long suiteId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String preconditions;

    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "priority_id")
    private Long priorityId;

    @Column(name = "estimate_seconds")
    private Integer estimateSeconds;

    @Column(name = "sort_index", nullable = false)
    @Builder.Default
    private int sortIndex = 0;

    /** Author (user.id) */
    @Column(name = "created_by")
    private Long createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @Column(name = "is_archived", nullable = false)
    private boolean archived = false;


    // Steps (action/expected/attachments/notes) — jsonb
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb")
    private List<Step> steps = new ArrayList<>();

    @Column(name = "expected_result", columnDefinition = "text")
    private String expectedResult;

    @Column(name = "actual_result", columnDefinition = "text")
    private String actualResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(name = "autotest_mapping", columnDefinition = "jsonb")
    private Map<String, String> autotestMapping = new HashMap<>();


    // attachments
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    @Column(columnDefinition = "jsonb")
    private List<Attachment> attachments = new ArrayList<>();

    @Column(name = "test_data", columnDefinition = "text")
    private String testData;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Severity severity = Severity.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "automation_status")
    @Builder.Default
    private AutomationStatus automationStatus = AutomationStatus.NOT_AUTOMATED;

    @Builder.Default
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags = new String[0];

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }


    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Step {
        private String action;
        private String expected;
        private String notes;
        @Builder.Default
        private List<Attachment> attachments = new ArrayList<>();
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Attachment {
        private String name;
        private String url;
        private Long size;
        private String mime;
    }

    public enum Status { DRAFT, READY, IN_PROGRESS, BLOCKED, PASSED, FAILED }
    public enum Severity { TRIVIAL, MINOR, NORMAL, MAJOR, CRITICAL }
    public enum AutomationStatus { NOT_AUTOMATED, IN_PROGRESS, AUTOMATED }
}

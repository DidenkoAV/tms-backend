package com.test.system.model.jira;

import com.test.system.model.group.Group;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "jira_connections")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class JiraConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false)
    private String baseUrl;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String tokenEncrypted;

    private String defaultProject;
    private String defaultIssueType;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

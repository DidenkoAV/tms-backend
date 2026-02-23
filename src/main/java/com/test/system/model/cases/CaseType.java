package com.test.system.model.cases;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "case_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(columnDefinition = "text")
    private String description;
}

package com.test.system.model.cases;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "priorities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Priority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(nullable = false)
    private int weight;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
}

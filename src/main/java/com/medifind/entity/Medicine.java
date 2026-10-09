package com.medifind.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_medicine_name", columnList = "name"),
    @Index(name = "idx_medicine_generic", columnList = "genericName"),
    @Index(name = "idx_medicine_brand", columnList = "brandName"),
    @Index(name = "idx_medicine_category", columnList = "category"),
    @Index(name = "idx_medicine_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String genericName;

    @Column(length = 150)
    private String brandName;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String dosageForm;

    @Column(nullable = false, length = 50)
    private String strength;

    @Column(length = 50)
    private String packSize;

    @Column(nullable = false, length = 120)
    private String manufacturer;

    @Column(columnDefinition = "TEXT")
    private String composition;

    @Builder.Default
    @Column(nullable = false)
    private Boolean prescriptionRequired = false;

    @Column(length = 255)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

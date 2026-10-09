package com.medifind.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "search_analytics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pharmacy_medicine_date", columnNames = {"pharmacy_id", "medicine_id", "date"})
    },
    indexes = {
        @Index(name = "idx_sa_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_sa_medicine", columnList = "medicine_id"),
        @Index(name = "idx_sa_date", columnList = "date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Builder.Default
    @Column(nullable = false)
    private Long searchCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private LocalDate date;
}

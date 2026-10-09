package com.medifind.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "pharmacy_hours",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pharmacy_day", columnNames = {"pharmacy_id", "dayOfWeek"})
    },
    indexes = {
        @Index(name = "idx_hours_pharmacy", columnList = "pharmacy_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    private LocalTime openingTime;

    private LocalTime closingTime;

    @Builder.Default
    @Column(nullable = false)
    private Boolean closed = false;
}

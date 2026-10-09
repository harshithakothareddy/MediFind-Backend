package com.medifind.entity;

import com.medifind.enums.AvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pharmacy_medicine", columnNames = {"pharmacy_id", "medicine_id"})
    },
    indexes = {
        @Index(name = "idx_inv_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_inv_medicine", columnList = "medicine_id"),
        @Index(name = "idx_inv_status", columnList = "availabilityStatus"),
        @Index(name = "idx_inv_price", columnList = "price"),
        @Index(name = "idx_inv_updated", columnList = "lastUpdatedAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Builder.Default
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer minimumStockLevel = 10;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 80)
    private String batchNumber;

    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityStatus availabilityStatus;

    @Builder.Default
    @Column(length = 50)
    private String dataSource = "PHARMACY_POS_SYNC";

    @Builder.Default
    @Column(length = 20)
    private String dataType = "LIVE";

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void updateAvailabilityStatus() {
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = LocalDateTime.now();
        }
        if (this.stockQuantity == null || this.stockQuantity <= 0
            || (this.expiryDate != null && this.expiryDate.isBefore(LocalDate.now()))) {
            this.stockQuantity = 0;
            this.availabilityStatus = AvailabilityStatus.OUT_OF_STOCK;
        } else if (this.minimumStockLevel != null && this.stockQuantity <= this.minimumStockLevel) {
            this.availabilityStatus = AvailabilityStatus.LOW_STOCK;
        } else {
            this.availabilityStatus = AvailabilityStatus.IN_STOCK;
        }
    }
}

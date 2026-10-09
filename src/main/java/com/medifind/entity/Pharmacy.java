package com.medifind.entity;

import com.medifind.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pharmacies", indexes = {
    @Index(name = "idx_pharmacy_city", columnList = "city"),
    @Index(name = "idx_pharmacy_area", columnList = "area"),
    @Index(name = "idx_pharmacy_coords", columnList = "latitude, longitude"),
    @Index(name = "idx_pharmacy_verified", columnList = "verified"),
    @Index(name = "idx_pharmacy_status", columnList = "verificationStatus")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 60)
    private String licenseNumber;

    @Column(length = 100)
    private String ownerName;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 80)
    private String area;

    @Column(nullable = false, length = 60)
    private String city;

    @Column(nullable = false, length = 60)
    private String state;

    @Column(nullable = false, length = 15)
    private String pincode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Double rating = 4.5;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalReviews = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private Boolean open24Hours = false;

    @Builder.Default
    @Column(length = 50)
    private String dataSource = "OpenStreetMap / Community Verified";

    @Builder.Default
    @Column(length = 20)
    private String dataType = "REAL";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pharmacy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PharmacyHours> operatingHours = new ArrayList<>();
}

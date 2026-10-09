package com.medifind.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_history", indexes = {
    @Index(name = "idx_sh_user", columnList = "user_id"),
    @Index(name = "idx_sh_searched_at", columnList = "searchedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @Column(nullable = false, length = 150)
    private String searchQuery;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime searchedAt;
}

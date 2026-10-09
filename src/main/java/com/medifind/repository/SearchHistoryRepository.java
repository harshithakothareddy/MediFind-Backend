package com.medifind.repository;

import com.medifind.entity.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    Page<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    List<SearchHistory> findTop10ByUserIdOrderBySearchedAtDesc(Long userId);

    void deleteByUserId(Long userId);
}

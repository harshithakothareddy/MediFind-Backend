package com.medifind.repository;

import com.medifind.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long>, JpaSpecificationExecutor<Medicine> {

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.active = true AND (
            LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(m.genericName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(m.brandName) LIKE LOWER(CONCAT('%', :query, '%')) OR
            LOWER(m.category) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        """)
    Page<Medicine> searchMedicines(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT m.category FROM Medicine m WHERE m.active = true ORDER BY m.category ASC")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT m.dosageForm FROM Medicine m WHERE m.active = true ORDER BY m.dosageForm ASC")
    List<String> findDistinctDosageForms();

    long countByActiveTrue();

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.active = true AND (
            LOWER(m.name) LIKE LOWER(CONCAT('%', :term, '%')) OR
            LOWER(m.genericName) LIKE LOWER(CONCAT('%', :term, '%')) OR
            LOWER(m.brandName) LIKE LOWER(CONCAT('%', :term, '%'))
        )
        """)
    List<Medicine> findByNameContainingIgnoreCase(@Param("term") String term);
}

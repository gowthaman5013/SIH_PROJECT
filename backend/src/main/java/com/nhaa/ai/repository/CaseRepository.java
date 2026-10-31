package com.nhaa.ai.repository;

import com.nhaa.ai.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<CaseEntity, String> {
    List<CaseEntity> findByStatus(String status);
    List<CaseEntity> findByFinalCategory(String finalCategory);
}

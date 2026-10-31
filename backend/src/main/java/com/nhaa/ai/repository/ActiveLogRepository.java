package com.nhaa.ai.repository;

import com.nhaa.ai.entity.ActiveLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActiveLogRepository extends JpaRepository<ActiveLogEntity, Long> {
    List<ActiveLogEntity> findByCaseIdOrderByTimestampAsc(String caseId);
    List<ActiveLogEntity> findAllByOrderByTimestampDesc();
}

package com.nhaa.ai.repository;

import com.nhaa.ai.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    List<AuditEventEntity> findByCaseIdOrderByTimestampAsc(String caseId);
    List<AuditEventEntity> findAllByOrderByTimestampDesc();
}

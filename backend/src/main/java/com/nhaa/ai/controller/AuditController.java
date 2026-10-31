package com.nhaa.ai.controller;

import com.nhaa.ai.entity.ActiveLogEntity;
import com.nhaa.ai.entity.AuditEventEntity;
import com.nhaa.ai.repository.ActiveLogRepository;
import com.nhaa.ai.repository.AuditEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditEventRepository auditEventRepository;
    private final ActiveLogRepository activeLogRepository;

    @Autowired
    public AuditController(AuditEventRepository auditEventRepository, ActiveLogRepository activeLogRepository) {
        this.auditEventRepository = auditEventRepository;
        this.activeLogRepository = activeLogRepository;
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditEventEntity>> getAllAuditEvents() {
        return ResponseEntity.ok(auditEventRepository.findAllByOrderByTimestampDesc());
    }

    @GetMapping("/cases/{caseId}/audit")
    public ResponseEntity<List<AuditEventEntity>> getAuditEventsByCase(@PathVariable String caseId) {
        return ResponseEntity.ok(auditEventRepository.findByCaseIdOrderByTimestampAsc(caseId));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<ActiveLogEntity>> getAllActiveLogs() {
        return ResponseEntity.ok(activeLogRepository.findAllByOrderByTimestampDesc());
    }

    @GetMapping("/cases/{caseId}/logs")
    public ResponseEntity<List<ActiveLogEntity>> getActiveLogsByCase(@PathVariable String caseId) {
        return ResponseEntity.ok(activeLogRepository.findByCaseIdOrderByTimestampAsc(caseId));
    }
}

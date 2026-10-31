package com.nhaa.ai.controller;

import com.nhaa.ai.entity.CaseEntity;
import com.nhaa.ai.service.CaseManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private final CaseManagementService caseManagementService;

    @Autowired
    public CaseController(CaseManagementService caseManagementService) {
        this.caseManagementService = caseManagementService;
    }

    @GetMapping
    public ResponseEntity<List<CaseEntity>> getAllCases() {
        return ResponseEntity.ok(caseManagementService.getAllCases());
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseEntity> getCaseById(@PathVariable String caseId) {
        return caseManagementService.getCaseById(caseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{caseId}/accept")
    public ResponseEntity<CaseEntity> acceptCase(@PathVariable String caseId, @RequestBody(required = false) Map<String, String> body) {
        String adminId = body != null ? body.get("adminId") : "ADMIN_01";
        String notes = body != null ? body.get("notes") : "Assessment accepted by administrator.";
        CaseEntity updated = caseManagementService.acceptCase(caseId, adminId, notes);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{caseId}/modify")
    public ResponseEntity<CaseEntity> modifyCase(@PathVariable String caseId, @RequestBody Map<String, String> body) {
        String adminId = body != null ? body.get("adminId") : "ADMIN_01";
        String category = body != null ? body.get("finalCategory") : "HIGH";
        String notes = body != null ? body.get("notes") : "Category modified by administrator.";
        CaseEntity updated = caseManagementService.modifyCase(caseId, adminId, category, notes);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{caseId}/escalate")
    public ResponseEntity<CaseEntity> escalateCase(@PathVariable String caseId, @RequestBody Map<String, String> body) {
        String adminId = body != null ? body.get("adminId") : "ADMIN_01";
        String reason = body != null ? body.get("reason") : "Emergency escalation triggered by administrator.";
        CaseEntity updated = caseManagementService.escalateCase(caseId, adminId, reason);
        return ResponseEntity.ok(updated);
    }
}

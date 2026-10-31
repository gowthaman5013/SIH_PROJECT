package com.nhaa.ai.controller;

import com.nhaa.ai.service.CaseManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/assessment")
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final CaseManagementService caseManagementService;

    @Autowired
    public AssessmentController(CaseManagementService caseManagementService) {
        this.caseManagementService = caseManagementService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAssessment(@RequestBody Map<String, String> payload) {
        String inputType = payload.getOrDefault("inputType", "TEXT");
        String text = payload.get("text");
        String audioPath = payload.get("audioPath");

        Map<String, Object> response = caseManagementService.processNewAssessment(inputType, text, audioPath);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> createTextAssessment(@RequestBody Map<String, String> payload) {
        String text = payload.get("text");
        Map<String, Object> response = caseManagementService.processNewAssessment("TEXT", text, null);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/audio")
    public ResponseEntity<Map<String, Object>> createAudioAssessment(@RequestBody Map<String, String> payload) {
        String audioPath = payload.getOrDefault("audioPath", "sample_audio.wav");
        Map<String, Object> response = caseManagementService.processNewAssessment("AUDIO", null, audioPath);
        return ResponseEntity.ok(response);
    }
}

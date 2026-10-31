package com.nhaa.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhaa.ai.dto.*;
import com.nhaa.ai.entity.ActiveLogEntity;
import com.nhaa.ai.entity.AuditEventEntity;
import com.nhaa.ai.entity.CaseEntity;
import com.nhaa.ai.repository.ActiveLogRepository;
import com.nhaa.ai.repository.AuditEventRepository;
import com.nhaa.ai.repository.CaseRepository;
import com.nhaa.ai.risk.dto.SafetyAssessmentResultDto;
import com.nhaa.ai.risk.dto.SupportRecommendationDto;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.service.safety.SafetyLayerService;
import com.nhaa.ai.risk.service.scoring.ContextScoringEngine;
import com.nhaa.ai.risk.service.scoring.SviCalculator;
import com.nhaa.ai.risk.service.scoring.TextScoringEngine;
import com.nhaa.ai.risk.service.scoring.VoiceScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CaseManagementService {

    private static final Logger logger = LoggerFactory.getLogger(CaseManagementService.class);

    private final CaseRepository caseRepository;
    private final AuditEventRepository auditEventRepository;
    private final ActiveLogRepository activeLogRepository;

    private final AudioProcessingService audioProcessingService;
    private final SarvamSttService sarvamSttService;
    private final PiiMaskingService piiMaskingService;
    private final StructuredContextService structuredContextService;
    private final TextMlClientService textMlClientService;
    private final VoiceMlClientService voiceMlClientService;
    private final ContextScoringEngine contextScoringEngine;
    private final TextScoringEngine textScoringEngine;
    private final VoiceScoringEngine voiceScoringEngine;
    private final SviCalculator sviCalculator;
    private final SafetyLayerService safetyLayerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public CaseManagementService(
            CaseRepository caseRepository,
            AuditEventRepository auditEventRepository,
            ActiveLogRepository activeLogRepository,
            AudioProcessingService audioProcessingService,
            SarvamSttService sarvamSttService,
            PiiMaskingService piiMaskingService,
            StructuredContextService structuredContextService,
            TextMlClientService textMlClientService,
            VoiceMlClientService voiceMlClientService,
            ContextScoringEngine contextScoringEngine,
            TextScoringEngine textScoringEngine,
            VoiceScoringEngine voiceScoringEngine,
            SviCalculator sviCalculator,
            SafetyLayerService safetyLayerService
    ) {
        this.caseRepository = caseRepository;
        this.auditEventRepository = auditEventRepository;
        this.activeLogRepository = activeLogRepository;
        this.audioProcessingService = audioProcessingService;
        this.sarvamSttService = sarvamSttService;
        this.piiMaskingService = piiMaskingService;
        this.structuredContextService = structuredContextService;
        this.textMlClientService = textMlClientService;
        this.voiceMlClientService = voiceMlClientService;
        this.contextScoringEngine = contextScoringEngine;
        this.textScoringEngine = textScoringEngine;
        this.voiceScoringEngine = voiceScoringEngine;
        this.sviCalculator = sviCalculator;
        this.safetyLayerService = safetyLayerService;
    }

    public Map<String, Object> processNewAssessment(String inputType, String rawText, String audioPath) {
        String caseId = "NHAA-" + String.format("%04d", new Random().nextInt(9000) + 1000);
        boolean isTextOnly = "TEXT".equalsIgnoreCase(inputType) || "TEXT_ONLY".equalsIgnoreCase(inputType);
        boolean isAudio = !isTextOnly && ("AUDIO".equalsIgnoreCase(inputType) || "MULTIMODAL".equalsIgnoreCase(inputType) || audioPath != null);
        String mode = isAudio ? "MULTIMODAL" : "TEXT_ONLY";
        
        logger.info("Initiating new victim assessment: caseId={}, inputType={}, mode={}", caseId, inputType, mode);

        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setCaseId(caseId);
        caseEntity.setAssessmentTimestamp(LocalDateTime.now());
        caseEntity.setInputType(mode);
        caseEntity.setStatus("PROCESSING");
        caseRepository.save(caseEntity);

        logEvent(caseId, "ASSESSMENT_INITIATED", "IN_PROGRESS", "System", "Input received and pipeline initialized for mode: " + mode, "INFO");
        recordAudit(caseId, "System", "AI_SYSTEM", "ASSESSMENT_CREATED", "NONE", "PROCESSING", "Initial complaint submission (" + mode + ")");

        String transcriptToAnalyze = rawText;
        if (isAudio) {
            String resolvedAudioFile = (audioPath != null && !audioPath.trim().isEmpty()) ? audioPath : (caseId + "_recording.wav");
            String resolvedAudioUrl = (audioPath != null && (audioPath.startsWith("/") || audioPath.startsWith("http")))
                    ? audioPath
                    : "/audio/" + (audioPath != null ? new java.io.File(audioPath).getName() : (caseId + "_recording.wav"));
            String resolvedFileName = new java.io.File(resolvedAudioFile).getName();

            caseEntity.setAudioUrl(resolvedAudioUrl);
            caseEntity.setAudioFileName(resolvedFileName);

            AudioProcessingService.AudioProcessingResult audioResult = audioProcessingService.processAudioInput(audioPath, null);
            logEvent(caseId, "AUDIO_PROCESSING", "COMPLETED", "AudioService", "Victim speech extracted from stereo channel", "INFO");

            SarvamSttService.SttResult sttResult = sarvamSttService.transcribeAudio(audioPath != null ? audioPath : caseId, "hi-IN");
            transcriptToAnalyze = sttResult.text;
            logEvent(caseId, "SPEECH_TO_TEXT", "COMPLETED", "SarvamSTT", "Transcribed victim speech via Sarvam Saaras STT", "INFO");
        }

        caseEntity.setRawInputText(rawText != null ? rawText : transcriptToAnalyze);

        // PII Masking
        PiiMaskingService.PiiMaskingResult piiResult = piiMaskingService.maskPii(transcriptToAnalyze);
        String sanitizedText = piiResult.getSanitizedTranscript();
        caseEntity.setSanitizedTranscript(sanitizedText);
        logEvent(caseId, "PII_MASKING", "COMPLETED", "PiiService", "PII masked successfully; sanitized transcript ready", "INFO");

        // Structured Context Extraction
        StructuredContextDto structuredContext = structuredContextService.extractStructuredContext(sanitizedText);
        try {
            caseEntity.setStructuredContextJson(objectMapper.writeValueAsString(structuredContext));
        } catch (Exception ignored) {}
        logEvent(caseId, "STRUCTURED_CONTEXT", "COMPLETED", "SarvamLLM", "Structured context extracted", "INFO");

        // Text ML (MuRIL)
        TextMlIndicatorsDto textMl = textMlClientService.analyzeTextNarrative(sanitizedText);
        double textRiskScore = textScoringEngine.calculateTextRiskScore(textMl);
        caseEntity.setTextRiskScore(textRiskScore);
        logEvent(caseId, "TEXT_ML", "COMPLETED", "MuRILService", "10 Text indicators computed, Text Risk Score: " + textRiskScore, "INFO");

        // Context Risk Score
        double contextRiskScore = contextScoringEngine.calculateContextRisk(structuredContext);
        caseEntity.setContextRiskScore(contextRiskScore);
        logEvent(caseId, "CONTEXT_RISK", "COMPLETED", "ContextScoringEngine", "Context Risk Score computed: " + contextRiskScore, "INFO");

        double voiceRiskScore = 0.0;
        VoiceMlIndicatorsDto voiceMl = new VoiceMlIndicatorsDto();
        SviResult sviResult;

        if (isAudio) {
            // Voice ML analysis for Audio/Multimodal mode
            voiceMl = voiceMlClientService.analyzeVoiceAudio(audioPath != null ? audioPath : caseId);
            voiceRiskScore = voiceScoringEngine.calculateVoiceRiskScore(voiceMl);
            caseEntity.setVoiceRiskScore(voiceRiskScore);
            logEvent(caseId, "VOICE_ML", "COMPLETED", "VoiceMLService", "7 Voice evidence metrics computed, Voice Risk Score: " + voiceRiskScore, "INFO");

            // Multimodal SVI Calculation (40% Text / 30% Voice / 30% Context)
            sviResult = sviCalculator.calculateSvi(textRiskScore, voiceRiskScore, contextRiskScore);
        } else {
            // TEXT-ONLY SVI Calculation: 70% Text ML + 30% Structured Context (NO Voice ML)
            sviResult = sviCalculator.calculateTextOnlySvi(textRiskScore, contextRiskScore);
            caseEntity.setVoiceRiskScore(null);
            logEvent(caseId, "TEXT_ONLY_SVI", "COMPLETED", "SviEngine", "Text-Only SVI computed (70% Text / 30% Context)", "INFO");
        }

        caseEntity.setSviValue(sviResult.getSviScore());
        caseEntity.setInitialCategory(sviResult.getRiskCategory().name());
        logEvent(caseId, "SVI_CALCULATED", "COMPLETED", "SviEngine", "SVI calculated: " + sviResult.getSviScore() + " (" + sviResult.getRiskCategory() + ")", "INFO");
        recordAudit(caseId, "System", "AI_SYSTEM", "SVI_CALCULATED", "PROCESSING", sviResult.getRiskCategory().name(), "Continuous SVI score generated");

        // Safety Validation Layer & Support Prioritization
        SafetyAssessmentResultDto safetyAssessment = safetyLayerService.evaluateSafety(sviResult, textMl, isAudio ? voiceMl : null, structuredContext);
        safetyAssessment.setInputMode(mode);
        caseEntity.setFinalCategory(safetyAssessment.getFinalRiskCategory().name());
        caseEntity.setSafetyOverrideTriggered(safetyAssessment.isSafetyEscalated());
        try {
            caseEntity.setSafetyAssessmentJson(objectMapper.writeValueAsString(safetyAssessment));
            if (safetyAssessment.getSupportPrioritization() != null) {
                caseEntity.setSupportPrioritizationJson(objectMapper.writeValueAsString(safetyAssessment.getSupportPrioritization()));
            }
        } catch (Exception ignored) {}

        if (safetyAssessment.isSafetyEscalated()) {
            logEvent(caseId, "SAFETY_OVERRIDE", "WARNING", "SafetyLayer", "Monotonic Safety Override triggered! Escalated to " + safetyAssessment.getFinalRiskCategory(), "WARN");
            recordAudit(caseId, "System", "SAFETY_LAYER", "SAFETY_OVERRIDE_TRIGGERED", sviResult.getRiskCategory().name(), safetyAssessment.getFinalRiskCategory().name(), "Safety critical indicators override baseline SVI");
        } else {
            logEvent(caseId, "SAFETY_VALIDATION", "COMPLETED", "SafetyLayer", "Safety validation completed. Risk Category: " + safetyAssessment.getFinalRiskCategory(), "INFO");
        }

        logEvent(caseId, "SUPPORT_PRIORITIZATION", "COMPLETED", "SupportEngine", "Support prioritization completed", "INFO");

        caseEntity.setStatus("AWAITING_ADMIN_REVIEW");
        caseRepository.save(caseEntity);
        logEvent(caseId, "PIPELINE_COMPLETE", "COMPLETED", "System", "Awaiting human administrator review", "INFO");

        // Construct Section 22 API Response Structure
        Map<String, Object> result = new HashMap<>();
        result.put("caseId", caseId);
        result.put("status", "AWAITING_ADMIN_REVIEW");
        result.put("mode", mode);
        result.put("input_mode", mode);

        Map<String, Object> textMlResponse = new HashMap<>();
        textMlResponse.put("risk_score", textRiskScore);
        textMlResponse.put("indicators", textMl);
        result.put("text_ml", textMlResponse);

        Map<String, Object> voiceMlResponse = new HashMap<>();
        voiceMlResponse.put("available", isAudio);
        voiceMlResponse.put("risk_score", isAudio ? voiceRiskScore : null);
        if (isAudio) voiceMlResponse.put("indicators", voiceMl);
        result.put("voice_ml", voiceMlResponse);

        Map<String, Object> structuredContextResponse = new HashMap<>();
        structuredContextResponse.put("facts", structuredContext);
        structuredContextResponse.put("risk_score", contextRiskScore);
        result.put("structured_context", structuredContextResponse);

        Map<String, Object> sviResponse = new HashMap<>();
        sviResponse.put("formula", isAudio ? "(Text × 0.40) + (Voice × 0.30) + (Context × 0.30)" : "(Text × 0.70) + (Context × 0.30)");
        sviResponse.put("input_mode", mode);
        sviResponse.put("text_weight", sviResult.getTextWeight());
        sviResponse.put("voice_weight", isAudio ? sviResult.getVoiceWeight() : 0.0);
        sviResponse.put("context_weight", sviResult.getContextWeight());
        sviResponse.put("score", sviResult.getSviScore());
        sviResponse.put("category", sviResult.getRiskCategory().name());
        sviResponse.put("risk_category", sviResult.getRiskCategory().name());
        result.put("svi", sviResponse);

        Map<String, Object> safetyResponse = new HashMap<>();
        safetyResponse.put("override_triggered", safetyAssessment.isSafetyEscalated());
        safetyResponse.put("priority", safetyAssessment.getPriority().name());
        safetyResponse.put("human_review_required", safetyAssessment.getHumanReviewRequired());
        safetyResponse.put("priority_alert", safetyAssessment.getPriorityAlert());
        safetyResponse.put("override_reason", safetyAssessment.getOverrideReason());
        safetyResponse.put("reasons", safetyAssessment.getEscalationReasons());
        safetyResponse.put("final_risk_category", safetyAssessment.getFinalRiskCategory().name());
        safetyResponse.put("safety_escalated", safetyAssessment.isSafetyEscalated());
        result.put("safety", safetyResponse);
        result.put("safety_assessment", safetyResponse);

        List<SupportRecommendationDto> recommendationsList = (safetyAssessment.getSupportPrioritization() != null)
                ? safetyAssessment.getSupportPrioritization().getRecommendations()
                : Collections.emptyList();
        result.put("support_prioritization", recommendationsList);

        Map<String, Object> validationResponse = new HashMap<>();
        validationResponse.put("status", "PROTOTYPE_NOT_CLINICALLY_VALIDATED");
        validationResponse.put("disclaimer", "AI-Assisted Decision-Support Prototype. Not a clinical diagnosis. Human review required.");
        result.put("validation", validationResponse);

        // Additional compatibility fields
        result.put("textRiskScore", textRiskScore);
        result.put("voiceRiskScore", isAudio ? voiceRiskScore : null);
        result.put("contextRiskScore", contextRiskScore);
        result.put("sanitizedTranscript", sanitizedText);
        result.put("rawInputText", rawText);
        result.put("audioUrl", caseEntity.getAudioUrl());
        result.put("audioFileName", caseEntity.getAudioFileName());
        result.put("safetyAssessment", safetyAssessment);
        result.put("disclaimer", "AI-assisted decision-support prototype. Not a medical/legal diagnosis. Human review required.");

        return result;
    }

    public CaseEntity acceptCase(String caseId, String adminId, String notes) {
        CaseEntity caseEntity = caseRepository.findById(caseId).orElseThrow();
        String oldStatus = caseEntity.getStatus();
        caseEntity.setStatus("ACCEPTED");
        caseEntity.setAdminId(adminId != null ? adminId : "ADMIN_01");
        caseEntity.setAdminAction("ACCEPT");
        caseEntity.setAdminReviewTimestamp(LocalDateTime.now());
        caseEntity.setAdminNotes(notes);
        caseRepository.save(caseEntity);

        recordAudit(caseId, adminId != null ? adminId : "ADMIN_01", "ADMINISTRATOR", "ADMIN_ACCEPTED", oldStatus, "ACCEPTED", notes != null ? notes : "Administrator confirmed AI assessment.");
        logEvent(caseId, "ADMIN_REVIEW", "COMPLETED", "AdminWorkflow", "Administrator accepted assessment.", "INFO");
        return caseEntity;
    }

    public CaseEntity modifyCase(String caseId, String adminId, String modifiedCategory, String notes) {
        CaseEntity caseEntity = caseRepository.findById(caseId).orElseThrow();
        String oldStatus = caseEntity.getStatus();
        String oldCategory = caseEntity.getFinalCategory();

        caseEntity.setStatus("MODIFIED");
        caseEntity.setFinalCategory(modifiedCategory);
        caseEntity.setAdminId(adminId != null ? adminId : "ADMIN_01");
        caseEntity.setAdminAction("MODIFY");
        caseEntity.setAdminReviewTimestamp(LocalDateTime.now());
        caseEntity.setAdminNotes(notes);
        caseRepository.save(caseEntity);

        recordAudit(caseId, adminId != null ? adminId : "ADMIN_01", "ADMINISTRATOR", "ADMIN_MODIFIED", oldCategory, modifiedCategory, notes != null ? notes : "Administrator modified risk category.");
        logEvent(caseId, "ADMIN_REVIEW", "COMPLETED", "AdminWorkflow", "Administrator modified risk category to " + modifiedCategory, "INFO");
        return caseEntity;
    }

    public CaseEntity escalateCase(String caseId, String adminId, String reason) {
        CaseEntity caseEntity = caseRepository.findById(caseId).orElseThrow();
        String oldStatus = caseEntity.getStatus();

        caseEntity.setStatus("ESCALATED");
        caseEntity.setFinalCategory("CRITICAL");
        caseEntity.setAdminId(adminId != null ? adminId : "ADMIN_01");
        caseEntity.setAdminAction("ESCALATE");
        caseEntity.setAdminReviewTimestamp(LocalDateTime.now());
        caseEntity.setAdminNotes(reason);
        caseRepository.save(caseEntity);

        recordAudit(caseId, adminId != null ? adminId : "ADMIN_01", "ADMINISTRATOR", "ADMIN_ESCALATED", oldStatus, "ESCALATED", reason != null ? reason : "High risk emergency escalation by administrator.");
        logEvent(caseId, "ADMIN_REVIEW", "COMPLETED", "AdminWorkflow", "Case escalated to emergency response by administrator.", "WARN");
        return caseEntity;
    }

    private void logEvent(String caseId, String stage, String status, String service, String msg, String severity) {
        activeLogRepository.save(new ActiveLogEntity(caseId, stage, status, service, msg, severity));
    }

    private void recordAudit(String caseId, String actor, String role, String action, String prev, String next, String reason) {
        auditEventRepository.save(new AuditEventEntity(caseId, actor, role, action, prev, next, reason));
    }

    public List<CaseEntity> getAllCases() {
        return caseRepository.findAll();
    }

    public Optional<CaseEntity> getCaseById(String caseId) {
        return caseRepository.findById(caseId);
    }
}

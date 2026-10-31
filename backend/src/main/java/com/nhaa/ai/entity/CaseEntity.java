package com.nhaa.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "victim_cases")
public class CaseEntity {

    @Id
    private String caseId;

    private LocalDateTime assessmentTimestamp;
    private String inputType; // TEXT, AUDIO, EXISTING_CASE

    @Column(length = 4000)
    private String rawInputText;

    @Column(length = 4000)
    private String sanitizedTranscript;

    private String status; // NEW, PROCESSING, ANALYSIS_COMPLETED, AWAITING_ADMIN_REVIEW, ACCEPTED, MODIFIED, ESCALATED, RESOLVED

    private Double textRiskScore;
    private Double voiceRiskScore;
    private Double contextRiskScore;
    private Double sviValue;
    private String initialCategory;
    private String finalCategory;
    private Boolean safetyOverrideTriggered;

    @Column(length = 4000)
    private String structuredContextJson;

    @Column(length = 4000)
    private String safetyAssessmentJson;

    @Column(length = 4000)
    private String supportPrioritizationJson;

    private String audioUrl;
    private String audioFileName;

    // Admin Review metadata
    private String adminId;
    private String adminAction; // ACCEPT, MODIFY, ESCALATE
    private LocalDateTime adminReviewTimestamp;

    @Column(length = 2000)
    private String adminNotes;

    public CaseEntity() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public LocalDateTime getAssessmentTimestamp() { return assessmentTimestamp; }
    public void setAssessmentTimestamp(LocalDateTime assessmentTimestamp) { this.assessmentTimestamp = assessmentTimestamp; }

    public String getInputType() { return inputType; }
    public void setInputType(String inputType) { this.inputType = inputType; }

    public String getRawInputText() { return rawInputText; }
    public void setRawInputText(String rawInputText) { this.rawInputText = rawInputText; }

    public String getSanitizedTranscript() { return sanitizedTranscript; }
    public void setSanitizedTranscript(String sanitizedTranscript) { this.sanitizedTranscript = sanitizedTranscript; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTextRiskScore() { return textRiskScore; }
    public void setTextRiskScore(Double textRiskScore) { this.textRiskScore = textRiskScore; }

    public Double getVoiceRiskScore() { return voiceRiskScore; }
    public void setVoiceRiskScore(Double voiceRiskScore) { this.voiceRiskScore = voiceRiskScore; }

    public Double getContextRiskScore() { return contextRiskScore; }
    public void setContextRiskScore(Double contextRiskScore) { this.contextRiskScore = contextRiskScore; }

    public Double getSviValue() { return sviValue; }
    public void setSviValue(Double sviValue) { this.sviValue = sviValue; }

    public String getInitialCategory() { return initialCategory; }
    public void setInitialCategory(String initialCategory) { this.initialCategory = initialCategory; }

    public String getFinalCategory() { return finalCategory; }
    public void setFinalCategory(String finalCategory) { this.finalCategory = finalCategory; }

    public Boolean getSafetyOverrideTriggered() { return safetyOverrideTriggered; }
    public void setSafetyOverrideTriggered(Boolean safetyOverrideTriggered) { this.safetyOverrideTriggered = safetyOverrideTriggered; }

    public String getStructuredContextJson() { return structuredContextJson; }
    public void setStructuredContextJson(String structuredContextJson) { this.structuredContextJson = structuredContextJson; }

    public String getSafetyAssessmentJson() { return safetyAssessmentJson; }
    public void setSafetyAssessmentJson(String safetyAssessmentJson) { this.safetyAssessmentJson = safetyAssessmentJson; }

    public String getSupportPrioritizationJson() { return supportPrioritizationJson; }
    public void setSupportPrioritizationJson(String supportPrioritizationJson) { this.supportPrioritizationJson = supportPrioritizationJson; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getAdminAction() { return adminAction; }
    public void setAdminAction(String adminAction) { this.adminAction = adminAction; }

    public LocalDateTime getAdminReviewTimestamp() { return adminReviewTimestamp; }
    public void setAdminReviewTimestamp(LocalDateTime adminReviewTimestamp) { this.adminReviewTimestamp = adminReviewTimestamp; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getAudioFileName() { return audioFileName; }
    public void setAudioFileName(String audioFileName) { this.audioFileName = audioFileName; }
}

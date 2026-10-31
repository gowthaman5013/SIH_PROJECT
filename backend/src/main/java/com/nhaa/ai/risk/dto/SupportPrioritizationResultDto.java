package com.nhaa.ai.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing the support prioritization outputs produced by
 * the Support Prioritization Engine in alignment with NHAA 14566 / SIH26093.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportPrioritizationResultDto {

    private SupportReviewItemDto counsellingReview;
    private SupportReviewItemDto rehabilitationReview;
    private SupportReviewItemDto legalAidReview;
    private SupportReviewItemDto medicalSupportReview;
    private SupportReviewItemDto protectionEmergencyReview;

    private List<SupportRecommendationDto> recommendations = new ArrayList<>();
    private SupportDebugTraceDto debugTrace;

    public SupportPrioritizationResultDto() {}

    public SupportPrioritizationResultDto(
            SupportReviewItemDto counsellingReview,
            SupportReviewItemDto rehabilitationReview,
            SupportReviewItemDto legalAidReview,
            SupportReviewItemDto medicalSupportReview,
            SupportReviewItemDto protectionEmergencyReview
    ) {
        this(counsellingReview, rehabilitationReview, legalAidReview, medicalSupportReview, protectionEmergencyReview, new ArrayList<>(), null);
    }

    public SupportPrioritizationResultDto(
            SupportReviewItemDto counsellingReview,
            SupportReviewItemDto rehabilitationReview,
            SupportReviewItemDto legalAidReview,
            SupportReviewItemDto medicalSupportReview,
            SupportReviewItemDto protectionEmergencyReview,
            List<SupportRecommendationDto> recommendations
    ) {
        this(counsellingReview, rehabilitationReview, legalAidReview, medicalSupportReview, protectionEmergencyReview, recommendations, null);
    }

    public SupportPrioritizationResultDto(
            SupportReviewItemDto counsellingReview,
            SupportReviewItemDto rehabilitationReview,
            SupportReviewItemDto legalAidReview,
            SupportReviewItemDto medicalSupportReview,
            SupportReviewItemDto protectionEmergencyReview,
            List<SupportRecommendationDto> recommendations,
            SupportDebugTraceDto debugTrace
    ) {
        this.counsellingReview = counsellingReview;
        this.rehabilitationReview = rehabilitationReview;
        this.legalAidReview = legalAidReview;
        this.medicalSupportReview = medicalSupportReview;
        this.protectionEmergencyReview = protectionEmergencyReview;
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
        this.debugTrace = debugTrace;
    }

    public List<SupportRecommendationDto> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<SupportRecommendationDto> recommendations) {
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public SupportDebugTraceDto getDebugTrace() {
        return debugTrace;
    }

    public void setDebugTrace(SupportDebugTraceDto debugTrace) {
        this.debugTrace = debugTrace;
    }

    public SupportReviewItemDto getCounsellingReview() {
        return counsellingReview;
    }

    public void setCounsellingReview(SupportReviewItemDto counsellingReview) {
        this.counsellingReview = counsellingReview;
    }

    public SupportReviewItemDto getRehabilitationReview() {
        return rehabilitationReview;
    }

    public void setRehabilitationReview(SupportReviewItemDto rehabilitationReview) {
        this.rehabilitationReview = rehabilitationReview;
    }

    public SupportReviewItemDto getLegalAidReview() {
        return legalAidReview;
    }

    public void setLegalAidReview(SupportReviewItemDto legalAidReview) {
        this.legalAidReview = legalAidReview;
    }

    public SupportReviewItemDto getMedicalSupportReview() {
        return medicalSupportReview;
    }

    public void setMedicalSupportReview(SupportReviewItemDto medicalSupportReview) {
        this.medicalSupportReview = medicalSupportReview;
    }

    public SupportReviewItemDto getProtectionEmergencyReview() {
        return protectionEmergencyReview;
    }

    public void setProtectionEmergencyReview(SupportReviewItemDto protectionEmergencyReview) {
        this.protectionEmergencyReview = protectionEmergencyReview;
    }
}


package com.nhaa.ai.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing an explicit multi-domain support recommendation item.
 * Supports categories: COUNSELLING, LEGAL_AID, MEDICAL_ASSISTANCE, POLICE_INTERVENTION, WITNESS_PROTECTION_CONSIDERATION, EMERGENCY_SUPPORT.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportRecommendationDto {

    private String support;
    private String priority;
    private String reason;
    private List<String> evidence = new ArrayList<>();
    private Boolean humanReviewRequired = false;

    public SupportRecommendationDto() {}

    public SupportRecommendationDto(String support, String priority, String reason, List<String> evidence, Boolean humanReviewRequired) {
        this.support = support;
        this.priority = priority;
        this.reason = reason;
        this.evidence = evidence != null ? evidence : new ArrayList<>();
        this.humanReviewRequired = humanReviewRequired != null ? humanReviewRequired : false;
    }

    public SupportRecommendationDto(String support, String priority, String reason, Boolean humanReviewRequired) {
        this(support, priority, reason, new ArrayList<>(), humanReviewRequired);
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence != null ? evidence : new ArrayList<>();
    }

    public Boolean getHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(Boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired != null ? humanReviewRequired : false;
    }
}

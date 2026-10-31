package com.nhaa.ai.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.model.SafetyPriority;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object representing the decision-support evaluation produced by the Safety Layer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafetyAssessmentResultDto {

    private String inputMode = "TEXT_ONLY";
    private Double sviValue;
    private RiskCategory initialRiskCategory;
    private RiskCategory finalRiskCategory;
    private Boolean safetyEscalated = false;
    private List<String> safetyIndicators = new ArrayList<>();
    private List<String> recommendedActions = new ArrayList<>();
    private SafetyPriority priority = SafetyPriority.ROUTINE;
    private Boolean humanReviewRequired = false;
    private Boolean priorityAlert = false;
    private String overrideReason;
    private List<String> escalationReasons = new ArrayList<>();
    private SupportPrioritizationResultDto supportPrioritization;

    public SafetyAssessmentResultDto() {}

    public SafetyAssessmentResultDto(Double sviValue,
                                     RiskCategory initialRiskCategory,
                                     RiskCategory finalRiskCategory,
                                     Boolean safetyEscalated,
                                     List<String> safetyIndicators,
                                     List<String> recommendedActions,
                                     SafetyPriority priority,
                                     Boolean humanReviewRequired,
                                     List<String> escalationReasons,
                                     SupportPrioritizationResultDto supportPrioritization) {
        this.sviValue = sviValue;
        this.initialRiskCategory = initialRiskCategory;
        this.finalRiskCategory = finalRiskCategory;
        this.safetyEscalated = safetyEscalated != null ? safetyEscalated : false;
        this.safetyIndicators = safetyIndicators != null ? safetyIndicators : new ArrayList<>();
        this.recommendedActions = recommendedActions != null ? recommendedActions : new ArrayList<>();
        this.priority = priority != null ? priority : SafetyPriority.ROUTINE;
        this.humanReviewRequired = humanReviewRequired != null ? humanReviewRequired : false;
        this.priorityAlert = (Boolean.TRUE.equals(safetyEscalated) || (safetyIndicators != null && (safetyIndicators.contains("IMMEDIATE_DANGER") || safetyIndicators.contains("SELF_HARM_SUICIDAL_IDEATION"))));
        this.overrideReason = (escalationReasons != null && !escalationReasons.isEmpty()) ? String.join("; ", escalationReasons) : null;
        this.escalationReasons = escalationReasons != null ? escalationReasons : new ArrayList<>();
        this.supportPrioritization = supportPrioritization;
    }

    public String getInputMode() {
        return inputMode;
    }

    public void setInputMode(String inputMode) {
        this.inputMode = inputMode;
    }

    public Boolean getPriorityAlert() {
        return priorityAlert;
    }

    public void setPriorityAlert(Boolean priorityAlert) {
        this.priorityAlert = priorityAlert != null ? priorityAlert : false;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public SupportPrioritizationResultDto getSupportPrioritization() {
        return supportPrioritization;
    }

    public void setSupportPrioritization(SupportPrioritizationResultDto supportPrioritization) {
        this.supportPrioritization = supportPrioritization;
    }

    public Double getSviValue() {
        return sviValue;
    }

    public void setSviValue(Double sviValue) {
        this.sviValue = sviValue;
    }

    public RiskCategory getInitialRiskCategory() {
        return initialRiskCategory;
    }

    public void setInitialRiskCategory(RiskCategory initialRiskCategory) {
        this.initialRiskCategory = initialRiskCategory;
    }

    public RiskCategory getFinalRiskCategory() {
        return finalRiskCategory;
    }

    public void setFinalRiskCategory(RiskCategory finalRiskCategory) {
        this.finalRiskCategory = finalRiskCategory;
    }

    public Boolean getSafetyEscalated() {
        return safetyEscalated;
    }

    public boolean isSafetyEscalated() {
        return Boolean.TRUE.equals(safetyEscalated);
    }

    public void setSafetyEscalated(Boolean safetyEscalated) {
        this.safetyEscalated = safetyEscalated != null ? safetyEscalated : false;
    }

    public List<String> getSafetyIndicators() {
        return safetyIndicators;
    }

    public void setSafetyIndicators(List<String> safetyIndicators) {
        this.safetyIndicators = safetyIndicators != null ? safetyIndicators : new ArrayList<>();
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> recommendedActions) {
        this.recommendedActions = recommendedActions != null ? recommendedActions : new ArrayList<>();
    }

    public SafetyPriority getPriority() {
        return priority;
    }

    public void setPriority(SafetyPriority priority) {
        this.priority = priority != null ? priority : SafetyPriority.ROUTINE;
    }

    public Boolean getHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(Boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired != null ? humanReviewRequired : false;
    }

    public List<String> getEscalationReasons() {
        return escalationReasons;
    }

    public void setEscalationReasons(List<String> escalationReasons) {
        this.escalationReasons = escalationReasons != null ? escalationReasons : new ArrayList<>();
    }
}


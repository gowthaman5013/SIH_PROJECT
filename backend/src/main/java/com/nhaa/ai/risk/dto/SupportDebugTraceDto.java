package com.nhaa.ai.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Debugging and audit trace object generated during Support Prioritization execution.
 * 
 * Captures:
 * 1. Input Indicators
 * 2. Triggered Rules
 * 3. Suppressed Rules
 * 4. Domain Reviews
 * 5. Final Recommendations
 * 6. Human Review
 * 7. Safety Override
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportDebugTraceDto {

    private Map<String, Object> inputIndicators = new LinkedHashMap<>();
    private List<String> triggeredRules = new ArrayList<>();
    private List<String> suppressedRules = new ArrayList<>();
    private Map<String, String> domainReviews = new LinkedHashMap<>();
    private List<String> finalRecommendations = new ArrayList<>();
    private Boolean humanReview = false;
    private Boolean safetyOverride = false;

    public SupportDebugTraceDto() {}

    public SupportDebugTraceDto(
            Map<String, Object> inputIndicators,
            List<String> triggeredRules,
            List<String> suppressedRules,
            Map<String, String> domainReviews,
            List<String> finalRecommendations,
            Boolean humanReview,
            Boolean safetyOverride
    ) {
        this.inputIndicators = inputIndicators != null ? inputIndicators : new LinkedHashMap<>();
        this.triggeredRules = triggeredRules != null ? triggeredRules : new ArrayList<>();
        this.suppressedRules = suppressedRules != null ? suppressedRules : new ArrayList<>();
        this.domainReviews = domainReviews != null ? domainReviews : new LinkedHashMap<>();
        this.finalRecommendations = finalRecommendations != null ? finalRecommendations : new ArrayList<>();
        this.humanReview = humanReview != null ? humanReview : false;
        this.safetyOverride = safetyOverride != null ? safetyOverride : false;
    }

    public Map<String, Object> getInputIndicators() {
        return inputIndicators;
    }

    public void setInputIndicators(Map<String, Object> inputIndicators) {
        this.inputIndicators = inputIndicators != null ? inputIndicators : new LinkedHashMap<>();
    }

    public List<String> getTriggeredRules() {
        return triggeredRules;
    }

    public void setTriggeredRules(List<String> triggeredRules) {
        this.triggeredRules = triggeredRules != null ? triggeredRules : new ArrayList<>();
    }

    public List<String> getSuppressedRules() {
        return suppressedRules;
    }

    public void setSuppressedRules(List<String> suppressedRules) {
        this.suppressedRules = suppressedRules != null ? suppressedRules : new ArrayList<>();
    }

    public Map<String, String> getDomainReviews() {
        return domainReviews;
    }

    public void setDomainReviews(Map<String, String> domainReviews) {
        this.domainReviews = domainReviews != null ? domainReviews : new LinkedHashMap<>();
    }

    public List<String> getFinalRecommendations() {
        return finalRecommendations;
    }

    public void setFinalRecommendations(List<String> finalRecommendations) {
        this.finalRecommendations = finalRecommendations != null ? finalRecommendations : new ArrayList<>();
    }

    public Boolean getHumanReview() {
        return humanReview;
    }

    public void setHumanReview(Boolean humanReview) {
        this.humanReview = humanReview != null ? humanReview : false;
    }

    public Boolean getSafetyOverride() {
        return safetyOverride;
    }

    public void setSafetyOverride(Boolean safetyOverride) {
        this.safetyOverride = safetyOverride != null ? safetyOverride : false;
    }
}

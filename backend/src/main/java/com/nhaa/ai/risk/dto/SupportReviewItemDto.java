package com.nhaa.ai.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single domain review output from the Support Prioritization Engine.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportReviewItemDto {

    private String status;
    private Integer priority;
    private String reason;
    private Boolean humanReviewRequired = false;

    public SupportReviewItemDto() {}

    public SupportReviewItemDto(String status, Integer priority, String reason, Boolean humanReviewRequired) {
        this.status = status;
        this.priority = priority != null ? priority : 0;
        this.reason = reason;
        this.humanReviewRequired = humanReviewRequired != null ? humanReviewRequired : false;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(Boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired != null ? humanReviewRequired : false;
    }
}

package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupportContextDto {

    private String supportAvailable = "UNKNOWN";
    private Boolean urgentHelpRequested = false;
    private Boolean medicalAssistanceRequested = false;

    public SupportContextDto() {}

    public SupportContextDto(String supportAvailable, Boolean urgentHelpRequested) {
        this.supportAvailable = supportAvailable != null ? supportAvailable : "UNKNOWN";
        this.urgentHelpRequested = urgentHelpRequested != null ? urgentHelpRequested : false;
    }

    public String getSupportAvailable() {
        return supportAvailable;
    }

    public void setSupportAvailable(String supportAvailable) {
        this.supportAvailable = supportAvailable;
    }

    public Boolean getUrgentHelpRequested() {
        return urgentHelpRequested;
    }

    public void setUrgentHelpRequested(Boolean urgentHelpRequested) {
        this.urgentHelpRequested = urgentHelpRequested;
    }

    public Boolean getMedicalAssistanceRequested() {
        return medicalAssistanceRequested;
    }

    public void setMedicalAssistanceRequested(Boolean medicalAssistanceRequested) {
        this.medicalAssistanceRequested = medicalAssistanceRequested;
    }
}

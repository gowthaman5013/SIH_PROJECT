package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalContextDto {

    private Boolean policeComplaintFiled = false;
    private Boolean firFiled = false;
    private Boolean courtCaseExists = false;
    private String legalProceedingStage = "UNKNOWN";
    private Boolean caseDelayMentioned = false;
    private Boolean legalHelpRequested = false;

    public LegalContextDto() {}

    public LegalContextDto(Boolean policeComplaintFiled, Boolean firFiled, Boolean courtCaseExists,
                           String legalProceedingStage, Boolean caseDelayMentioned, Boolean legalHelpRequested) {
        this.policeComplaintFiled = policeComplaintFiled != null ? policeComplaintFiled : false;
        this.firFiled = firFiled != null ? firFiled : false;
        this.courtCaseExists = courtCaseExists != null ? courtCaseExists : false;
        this.legalProceedingStage = legalProceedingStage != null ? legalProceedingStage : "UNKNOWN";
        this.caseDelayMentioned = caseDelayMentioned != null ? caseDelayMentioned : false;
        this.legalHelpRequested = legalHelpRequested != null ? legalHelpRequested : false;
    }

    public Boolean getPoliceComplaintFiled() {
        return policeComplaintFiled;
    }

    public void setPoliceComplaintFiled(Boolean policeComplaintFiled) {
        this.policeComplaintFiled = policeComplaintFiled;
    }

    public Boolean getFirFiled() {
        return firFiled;
    }

    public void setFirFiled(Boolean firFiled) {
        this.firFiled = firFiled;
    }

    public Boolean getCourtCaseExists() {
        return courtCaseExists;
    }

    public void setCourtCaseExists(Boolean courtCaseExists) {
        this.courtCaseExists = courtCaseExists;
    }

    public String getLegalProceedingStage() {
        return legalProceedingStage;
    }

    public void setLegalProceedingStage(String legalProceedingStage) {
        this.legalProceedingStage = legalProceedingStage;
    }

    public Boolean getCaseDelayMentioned() {
        return caseDelayMentioned;
    }

    public void setCaseDelayMentioned(Boolean caseDelayMentioned) {
        this.caseDelayMentioned = caseDelayMentioned;
    }

    public Boolean getLegalHelpRequested() {
        return legalHelpRequested;
    }

    public void setLegalHelpRequested(Boolean legalHelpRequested) {
        this.legalHelpRequested = legalHelpRequested;
    }
}

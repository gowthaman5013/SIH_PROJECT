package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PropertyContextDto {

    private Boolean propertyIssue = false;
    private String propertyType = "";
    private Boolean ownershipDispute = false;
    private Boolean possessionIssue = false;

    public PropertyContextDto() {}

    public PropertyContextDto(Boolean propertyIssue, String propertyType,
                              Boolean ownershipDispute, Boolean possessionIssue) {
        this.propertyIssue = propertyIssue != null ? propertyIssue : false;
        this.propertyType = propertyType != null ? propertyType : "";
        this.ownershipDispute = ownershipDispute != null ? ownershipDispute : false;
        this.possessionIssue = possessionIssue != null ? possessionIssue : false;
    }

    public Boolean getPropertyIssue() {
        return propertyIssue;
    }

    public void setPropertyIssue(Boolean propertyIssue) {
        this.propertyIssue = propertyIssue;
    }

    public Boolean getPropertyDispossession() {
        return Boolean.TRUE.equals(propertyIssue) || Boolean.TRUE.equals(possessionIssue);
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public Boolean getOwnershipDispute() {
        return ownershipDispute;
    }

    public void setOwnershipDispute(Boolean ownershipDispute) {
        this.ownershipDispute = ownershipDispute;
    }

    public Boolean getPossessionIssue() {
        return possessionIssue;
    }

    public void setPossessionIssue(Boolean possessionIssue) {
        this.possessionIssue = possessionIssue;
    }
}

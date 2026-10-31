package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FamilyContextDto {

    private Boolean familyInvolved = false;
    private Boolean familyMemberThreatened = false;
    private Boolean familyMemberHarmed = false;

    public FamilyContextDto() {}

    public FamilyContextDto(Boolean familyInvolved, Boolean familyMemberThreatened, Boolean familyMemberHarmed) {
        this.familyInvolved = familyInvolved != null ? familyInvolved : false;
        this.familyMemberThreatened = familyMemberThreatened != null ? familyMemberThreatened : false;
        this.familyMemberHarmed = familyMemberHarmed != null ? familyMemberHarmed : false;
    }

    public Boolean getFamilyInvolved() {
        return familyInvolved;
    }

    public void setFamilyInvolved(Boolean familyInvolved) {
        this.familyInvolved = familyInvolved != null ? familyInvolved : false;
    }

    public Boolean getFamilyMemberThreatened() {
        return familyMemberThreatened;
    }

    public void setFamilyMemberThreatened(Boolean familyMemberThreatened) {
        this.familyMemberThreatened = familyMemberThreatened != null ? familyMemberThreatened : false;
    }

    public Boolean getFamilyMemberHarmed() {
        return familyMemberHarmed;
    }

    public void setFamilyMemberHarmed(Boolean familyMemberHarmed) {
        this.familyMemberHarmed = familyMemberHarmed != null ? familyMemberHarmed : false;
    }
}

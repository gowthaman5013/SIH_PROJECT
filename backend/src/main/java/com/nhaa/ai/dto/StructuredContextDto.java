package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredContextDto {

    private IncidentContextDto incidentContext = new IncidentContextDto();
    private VictimContextDto victimContext = new VictimContextDto();
    private PerpetratorContextDto perpetratorContext = new PerpetratorContextDto();
    private SafetyContextDto safetyContext = new SafetyContextDto();
    private FamilyContextDto familyContext = new FamilyContextDto();
    private SocialContextDto socialContext = new SocialContextDto();
    private PropertyContextDto propertyContext = new PropertyContextDto();
    private LegalContextDto legalContext = new LegalContextDto();
    private SupportContextDto supportContext = new SupportContextDto();

    public StructuredContextDto() {}

    public StructuredContextDto(IncidentContextDto incidentContext, VictimContextDto victimContext,
                                PerpetratorContextDto perpetratorContext, SafetyContextDto safetyContext,
                                FamilyContextDto familyContext, SocialContextDto socialContext,
                                PropertyContextDto propertyContext, LegalContextDto legalContext,
                                SupportContextDto supportContext) {
        this.incidentContext = incidentContext != null ? incidentContext : new IncidentContextDto();
        this.victimContext = victimContext != null ? victimContext : new VictimContextDto();
        this.perpetratorContext = perpetratorContext != null ? perpetratorContext : new PerpetratorContextDto();
        this.safetyContext = safetyContext != null ? safetyContext : new SafetyContextDto();
        this.familyContext = familyContext != null ? familyContext : new FamilyContextDto();
        this.socialContext = socialContext != null ? socialContext : new SocialContextDto();
        this.propertyContext = propertyContext != null ? propertyContext : new PropertyContextDto();
        this.legalContext = legalContext != null ? legalContext : new LegalContextDto();
        this.supportContext = supportContext != null ? supportContext : new SupportContextDto();
    }

    public IncidentContextDto getIncidentContext() {
        return incidentContext;
    }

    public void setIncidentContext(IncidentContextDto incidentContext) {
        this.incidentContext = incidentContext != null ? incidentContext : new IncidentContextDto();
    }

    public VictimContextDto getVictimContext() {
        return victimContext;
    }

    public void setVictimContext(VictimContextDto victimContext) {
        this.victimContext = victimContext != null ? victimContext : new VictimContextDto();
    }

    public PerpetratorContextDto getPerpetratorContext() {
        return perpetratorContext;
    }

    public void setPerpetratorContext(PerpetratorContextDto perpetratorContext) {
        this.perpetratorContext = perpetratorContext != null ? perpetratorContext : new PerpetratorContextDto();
    }

    public SafetyContextDto getSafetyContext() {
        return safetyContext;
    }

    public void setSafetyContext(SafetyContextDto safetyContext) {
        this.safetyContext = safetyContext != null ? safetyContext : new SafetyContextDto();
    }

    public FamilyContextDto getFamilyContext() {
        return familyContext;
    }

    public void setFamilyContext(FamilyContextDto familyContext) {
        this.familyContext = familyContext != null ? familyContext : new FamilyContextDto();
    }

    public SocialContextDto getSocialContext() {
        return socialContext;
    }

    public void setSocialContext(SocialContextDto socialContext) {
        this.socialContext = socialContext != null ? socialContext : new SocialContextDto();
    }

    public PropertyContextDto getPropertyContext() {
        return propertyContext;
    }

    public void setPropertyContext(PropertyContextDto propertyContext) {
        this.propertyContext = propertyContext != null ? propertyContext : new PropertyContextDto();
    }

    public LegalContextDto getLegalContext() {
        return legalContext;
    }

    public void setLegalContext(LegalContextDto legalContext) {
        this.legalContext = legalContext != null ? legalContext : new LegalContextDto();
    }

    public SupportContextDto getSupportContext() {
        return supportContext;
    }

    public void setSupportContext(SupportContextDto supportContext) {
        this.supportContext = supportContext != null ? supportContext : new SupportContextDto();
    }
}

package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TextMlIndicatorsDto {

    @JsonProperty("physical_violence")
    private Double physicalViolence;

    @JsonProperty("threat_intimidation")
    private Double threatIntimidation;

    @JsonProperty("weapon_life_threat")
    private Double weaponLifeThreat;

    @JsonProperty("family_abuse")
    private Double familyAbuse;

    @JsonProperty("financial_deprivation")
    private Double financialDeprivation;

    @JsonProperty("property_dispossession")
    private Double propertyDispossession;

    @JsonProperty("stalking_cyber_harassment")
    private Double stalkingCyberHarassment;

    @JsonProperty("child_dependent_exposure")
    private Double childDependentExposure;

    @JsonProperty("repeat_offence")
    private Double repeatOffence;

    @JsonProperty("institutional_denial")
    private Double institutionalDenial;

    public TextMlIndicatorsDto() {}

    public TextMlIndicatorsDto(Double physicalViolence, Double threatIntimidation, Double weaponLifeThreat,
                               Double familyAbuse, Double financialDeprivation, Double propertyDispossession,
                               Double stalkingCyberHarassment, Double childDependentExposure, Double repeatOffence,
                               Double institutionalDenial) {
        this.physicalViolence = physicalViolence;
        this.threatIntimidation = threatIntimidation;
        this.weaponLifeThreat = weaponLifeThreat;
        this.familyAbuse = familyAbuse;
        this.financialDeprivation = financialDeprivation;
        this.propertyDispossession = propertyDispossession;
        this.stalkingCyberHarassment = stalkingCyberHarassment;
        this.childDependentExposure = childDependentExposure;
        this.repeatOffence = repeatOffence;
        this.institutionalDenial = institutionalDenial;
    }

    public Double getPhysicalViolence() { return physicalViolence; }
    public void setPhysicalViolence(Double physicalViolence) { this.physicalViolence = physicalViolence; }

    public Double getThreatIntimidation() { return threatIntimidation; }
    public void setThreatIntimidation(Double threatIntimidation) { this.threatIntimidation = threatIntimidation; }

    public Double getWeaponLifeThreat() { return weaponLifeThreat; }
    public void setWeaponLifeThreat(Double weaponLifeThreat) { this.weaponLifeThreat = weaponLifeThreat; }

    public Double getFamilyAbuse() { return familyAbuse; }
    public void setFamilyAbuse(Double familyAbuse) { this.familyAbuse = familyAbuse; }

    public Double getFinancialDeprivation() { return financialDeprivation; }
    public void setFinancialDeprivation(Double financialDeprivation) { this.financialDeprivation = financialDeprivation; }

    public Double getPropertyDispossession() { return propertyDispossession; }
    public void setPropertyDispossession(Double propertyDispossession) { this.propertyDispossession = propertyDispossession; }

    public Double getStalkingCyberHarassment() { return stalkingCyberHarassment; }
    public void setStalkingCyberHarassment(Double stalkingCyberHarassment) { this.stalkingCyberHarassment = stalkingCyberHarassment; }

    public Double getChildDependentExposure() { return childDependentExposure; }
    public void setChildDependentExposure(Double childDependentExposure) { this.childDependentExposure = childDependentExposure; }

    public Double getRepeatOffence() { return repeatOffence; }
    public void setRepeatOffence(Double repeatOffence) { this.repeatOffence = repeatOffence; }

    public Double getInstitutionalDenial() { return institutionalDenial; }
    public void setInstitutionalDenial(Double institutionalDenial) { this.institutionalDenial = institutionalDenial; }

    // Legacy getters and setters for backward compatibility
    public Double getViolence() { return physicalViolence; }
    public void setViolence(Double v) { this.physicalViolence = v; }

    public Double getSexualViolence() { return weaponLifeThreat; }
    public void setSexualViolence(Double sv) { this.weaponLifeThreat = sv; }

    public Double getImmediateDanger() { return weaponLifeThreat != null ? weaponLifeThreat : threatIntimidation; }
    public void setImmediateDanger(Double id) { this.weaponLifeThreat = id; }

    public Double getSocialIsolation() { return stalkingCyberHarassment; }
    public void setSocialIsolation(Double si) { this.stalkingCyberHarassment = si; }

    public Double getDisplacement() { return propertyDispossession; }
    public void setDisplacement(Double d) { this.propertyDispossession = d; }

    public Double getFamilyHarm() { return familyAbuse; }
    public void setFamilyHarm(Double fh) { this.familyAbuse = fh; }

    public Double getSelfHarmSuicidalIdeation() { return weaponLifeThreat; }
    public void setSelfHarmSuicidalIdeation(Double sh) { this.weaponLifeThreat = sh; }

    public Double getDepressionRelated() { return stalkingCyberHarassment; }
    public void setDepressionRelated(Double dr) { this.stalkingCyberHarassment = dr; }

    public Double getLegalDistress() { return institutionalDenial; }
    public void setLegalDistress(Double ld) { this.institutionalDenial = ld; }

    public double getProbability(String key) {
        if (key == null) return 0.0;
        Double val = switch (key) {
            case "physical_violence", "violence" -> physicalViolence;
            case "threat_intimidation" -> threatIntimidation;
            case "weapon_life_threat", "sexual_violence", "immediate_danger", "self_harm_suicidal_ideation", "self_harm" -> weaponLifeThreat;
            case "family_abuse", "family_harm" -> familyAbuse;
            case "financial_deprivation" -> financialDeprivation;
            case "property_dispossession", "displacement" -> propertyDispossession;
            case "stalking_cyber_harassment", "social_isolation", "depression_related" -> stalkingCyberHarassment;
            case "child_dependent_exposure" -> childDependentExposure;
            case "repeat_offence" -> repeatOffence;
            case "institutional_denial", "legal_distress" -> institutionalDenial;
            default -> 0.0;
        };
        return val != null ? val : 0.0;
    }
}

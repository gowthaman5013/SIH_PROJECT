package com.nhaa.ai.risk.dto;

import com.nhaa.ai.risk.model.RiskCategory;

public class SviResult {

    private Double textRisk;
    private Double voiceRisk;
    private Double contextRisk;

    private Double textWeight;
    private Double voiceWeight;
    private Double contextWeight;

    private Double normalizedSvi;
    private Double sviScore;
    private RiskCategory riskCategory;

    public SviResult() {}

    public SviResult(Double textRisk, Double voiceRisk, Double contextRisk,
                     Double textWeight, Double voiceWeight, Double contextWeight,
                     Double normalizedSvi, Double sviScore, RiskCategory riskCategory) {
        this.textRisk = textRisk;
        this.voiceRisk = voiceRisk;
        this.contextRisk = contextRisk;
        this.textWeight = textWeight;
        this.voiceWeight = voiceWeight;
        this.contextWeight = contextWeight;
        this.normalizedSvi = normalizedSvi;
        this.sviScore = sviScore;
        this.riskCategory = riskCategory;
    }

    public Double getTextRisk() { return textRisk; }
    public void setTextRisk(Double textRisk) { this.textRisk = textRisk; }

    public Double getVoiceRisk() { return voiceRisk; }
    public void setVoiceRisk(Double voiceRisk) { this.voiceRisk = voiceRisk; }

    public Double getContextRisk() { return contextRisk; }
    public void setContextRisk(Double contextRisk) { this.contextRisk = contextRisk; }

    public Double getTextWeight() { return textWeight; }
    public void setTextWeight(Double textWeight) { this.textWeight = textWeight; }

    public Double getVoiceWeight() { return voiceWeight; }
    public void setVoiceWeight(Double voiceWeight) { this.voiceWeight = voiceWeight; }

    public Double getContextWeight() { return contextWeight; }
    public void setContextWeight(Double contextWeight) { this.contextWeight = contextWeight; }

    public Double getNormalizedSvi() { return normalizedSvi; }
    public void setNormalizedSvi(Double normalizedSvi) { this.normalizedSvi = normalizedSvi; }

    public Double getSviScore() { return sviScore; }
    public void setSviScore(Double sviScore) { this.sviScore = sviScore; }

    public RiskCategory getRiskCategory() { return riskCategory; }
    public void setRiskCategory(RiskCategory riskCategory) { this.riskCategory = riskCategory; }
}

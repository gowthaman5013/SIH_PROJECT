package com.nhaa.ai.risk.config;

import org.springframework.context.annotation.Configuration;

/**
 * Configurable weights and threshold parameters for SVI and Risk Engines.
 * 
 * DISCLAIMER: PROTOTYPE WEIGHTS — REQUIRE DOMAIN EXPERT REVIEW AND VALIDATION. NOT CLINICALLY VALIDATED.
 */
@Configuration
public class SviConfiguration {

    // Multimodal SVI Weighting (Must sum to 1.0)
    public static final double DEFAULT_TEXT_WEIGHT = 0.40;
    public static final double DEFAULT_VOICE_WEIGHT = 0.30;
    public static final double DEFAULT_CONTEXT_WEIGHT = 0.30;

    // Text-Only SVI Weighting (Must sum to 1.0)
    public static final double DEFAULT_TEXT_ONLY_TEXT_WEIGHT = 0.70;
    public static final double DEFAULT_TEXT_ONLY_CONTEXT_WEIGHT = 0.30;

    // SVI Category Boundaries (0-100)
    // 0 - 24   -> LOW
    // 25 - 49  -> MODERATE
    // 50 - 74  -> HIGH
    // 75 - 100 -> CRITICAL
    public static final double CATEGORY_MODERATE_MIN = 25.0;
    public static final double CATEGORY_HIGH_MIN = 50.0;
    public static final double CATEGORY_CRITICAL_MIN = 75.0;

    // Legacy threshold constants retained for backwards compatibility
    public static final double LOW_THRESHOLD = 24.0;
    public static final double MODERATE_THRESHOLD = 49.0;
    public static final double HIGH_THRESHOLD = 74.0;

    public static final String PARAMETER_DISCLAIMER =
            "PROTOTYPE WEIGHTS — REQUIRE DOMAIN EXPERT REVIEW AND VALIDATION. NOT CLINICALLY VALIDATED.";

    private double textWeight = DEFAULT_TEXT_WEIGHT;
    private double voiceWeight = DEFAULT_VOICE_WEIGHT;
    private double contextWeight = DEFAULT_CONTEXT_WEIGHT;

    private double textOnlyTextWeight = DEFAULT_TEXT_ONLY_TEXT_WEIGHT;
    private double textOnlyContextWeight = DEFAULT_TEXT_ONLY_CONTEXT_WEIGHT;

    public double getTextWeight() {
        return textWeight;
    }

    public void setTextWeight(double textWeight) {
        this.textWeight = textWeight;
    }

    public double getVoiceWeight() {
        return voiceWeight;
    }

    public void setVoiceWeight(double voiceWeight) {
        this.voiceWeight = voiceWeight;
    }

    public double getContextWeight() {
        return contextWeight;
    }

    public void setContextWeight(double contextWeight) {
        this.contextWeight = contextWeight;
    }

    public double getTextOnlyTextWeight() {
        return textOnlyTextWeight;
    }

    public void setTextOnlyTextWeight(double textOnlyTextWeight) {
        this.textOnlyTextWeight = textOnlyTextWeight;
    }

    public double getTextOnlyContextWeight() {
        return textOnlyContextWeight;
    }

    public void setTextOnlyContextWeight(double textOnlyContextWeight) {
        this.textOnlyContextWeight = textOnlyContextWeight;
    }

    public boolean validateTextOnlyWeights() {
        return Math.abs((textOnlyTextWeight + textOnlyContextWeight) - 1.0) < 0.001;
    }
}


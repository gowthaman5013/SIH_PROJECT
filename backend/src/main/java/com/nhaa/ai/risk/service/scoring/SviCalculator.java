package com.nhaa.ai.risk.service.scoring;

import com.nhaa.ai.risk.config.SviConfiguration;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.model.RiskCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Social Vulnerability Index (SVI) Calculation Engine.
 * 
 * Supports both TEXT_ONLY (70% Text ML + 30% Structured Context) and MULTIMODAL modes.
 * Fuses normalized risk scores into continuous 0-100 SVI.
 * Categorization thresholds:
 * - 0 - 24   -> LOW
 * - 25 - 49  -> MODERATE
 * - 50 - 74  -> HIGH
 * - 75 - 100 -> CRITICAL
 * 
 * DISCLAIMER: PROTOTYPE WEIGHTS — REQUIRE DOMAIN EXPERT REVIEW AND VALIDATION. NOT CLINICALLY VALIDATED.
 */
@Service
public class SviCalculator {

    private final SviConfiguration sviConfig;

    @Autowired
    public SviCalculator(SviConfiguration sviConfig) {
        this.sviConfig = sviConfig != null ? sviConfig : new SviConfiguration();
    }

    /**
     * Text-Only SVI Calculation (Mode A).
     * Formula: SVI = (Text ML Risk Score * 0.70) + (Structured Context Risk Score * 0.30)
     */
    public SviResult calculateTextOnlySvi(double textRisk, double contextRisk) {
        double textW = sviConfig.getTextOnlyTextWeight();
        double contextW = sviConfig.getTextOnlyContextWeight();

        // Validate and normalize weights if needed
        double totalW = textW + contextW;
        if (totalW > 0.0 && Math.abs(totalW - 1.0) > 0.001) {
            textW /= totalW;
            contextW /= totalW;
        }

        double rawSvi = (textRisk * textW) + (contextRisk * contextW);
        double sviScore = Math.min(100.0, Math.max(0.0, Math.round(rawSvi * 100.0) / 100.0));
        double normalizedSvi = Math.round((sviScore / 100.0) * 10000.0) / 10000.0;

        RiskCategory category = categorizeRisk(sviScore);

        return new SviResult(
                textRisk,
                0.0,
                contextRisk,
                textW,
                0.0,
                contextW,
                normalizedSvi,
                sviScore,
                category
        );
    }

    /**
     * Multimodal SVI Calculation (Text + Voice + Context).
     */
    public SviResult calculateSvi(double textRisk, double voiceRisk, double contextRisk) {
        double textW = sviConfig.getTextWeight();
        double voiceW = sviConfig.getVoiceWeight();
        double contextW = sviConfig.getContextWeight();

        // Ensure weights sum to 1.0
        double totalW = textW + voiceW + contextW;
        if (totalW > 0.0 && Math.abs(totalW - 1.0) > 0.001) {
            textW /= totalW;
            voiceW /= totalW;
            contextW /= totalW;
        }

        double rawSvi = (textRisk * textW) + (voiceRisk * voiceW) + (contextRisk * contextW);
        double sviScore = Math.min(100.0, Math.max(0.0, Math.round(rawSvi * 100.0) / 100.0));
        double normalizedSvi = Math.round((sviScore / 100.0) * 10000.0) / 10000.0;

        RiskCategory category = categorizeRisk(sviScore);

        return new SviResult(
                textRisk,
                voiceRisk,
                contextRisk,
                textW,
                voiceW,
                contextW,
                normalizedSvi,
                sviScore,
                category
        );
    }

    /**
     * Centralized Risk Categorization source of truth.
     * 0-24   -> LOW
     * 25-49  -> MODERATE
     * 50-74  -> HIGH
     * 75-100 -> CRITICAL
     */
    public RiskCategory categorizeRisk(double sviScore) {
        if (sviScore >= SviConfiguration.CATEGORY_CRITICAL_MIN) { // 75.0
            return RiskCategory.CRITICAL;
        } else if (sviScore >= SviConfiguration.CATEGORY_HIGH_MIN) { // 50.0
            return RiskCategory.HIGH;
        } else if (sviScore >= SviConfiguration.CATEGORY_MODERATE_MIN) { // 25.0
            return RiskCategory.MODERATE;
        } else {
            return RiskCategory.LOW;
        }
    }

    public RiskCategory determineCategory(double sviScore) {
        return categorizeRisk(sviScore);
    }
}


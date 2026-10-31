package com.nhaa.ai.risk;

import com.nhaa.ai.risk.config.SviConfiguration;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.service.scoring.SviCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SviCalculatorTest {

    private SviCalculator sviCalculator;

    @BeforeEach
    void setUp() {
        SviConfiguration config = new SviConfiguration();
        config.setTextWeight(0.40);
        config.setVoiceWeight(0.30);
        config.setContextWeight(0.30);
        config.setTextOnlyTextWeight(0.70);
        config.setTextOnlyContextWeight(0.30);
        sviCalculator = new SviCalculator(config);
    }

    @Test
    @DisplayName("SVI Calculator: Text-Only formula check (Text 62 * 0.70 + Context 70 * 0.30 = 64.40 HIGH)")
    void testTextOnlySviCalculation() {
        SviResult result = sviCalculator.calculateTextOnlySvi(62.0, 70.0);

        assertEquals(64.40, result.getSviScore());
        assertEquals(RiskCategory.HIGH, result.getRiskCategory());
        assertEquals(0.70, result.getTextWeight());
        assertEquals(0.30, result.getContextWeight());
        assertEquals(0.0, result.getVoiceWeight());
    }

    @Test
    @DisplayName("SVI Calculator: Multimodal 3-input fusion weighted sum")
    void testMultimodalSviCalculation() {
        // Text: 50.0 (40%), Voice: 60.0 (30%), Context: 70.0 (30%)
        // 50*0.4 + 60*0.3 + 70*0.3 = 20 + 18 + 21 = 59.0 (HIGH)
        SviResult result = sviCalculator.calculateSvi(50.0, 60.0, 70.0);

        assertEquals(59.0, result.getSviScore());
        assertEquals(RiskCategory.HIGH, result.getRiskCategory());
    }

    @Test
    @DisplayName("SVI Categorization: 0-24 LOW, 25-49 MODERATE, 50-74 HIGH, 75-100 CRITICAL")
    void testCategorizationThresholds() {
        assertEquals(RiskCategory.LOW, sviCalculator.categorizeRisk(15.0));
        assertEquals(RiskCategory.LOW, sviCalculator.categorizeRisk(24.0));
        assertEquals(RiskCategory.MODERATE, sviCalculator.categorizeRisk(25.0));
        assertEquals(RiskCategory.MODERATE, sviCalculator.categorizeRisk(49.0));
        assertEquals(RiskCategory.HIGH, sviCalculator.categorizeRisk(50.0));
        assertEquals(RiskCategory.HIGH, sviCalculator.categorizeRisk(74.0));
        assertEquals(RiskCategory.CRITICAL, sviCalculator.categorizeRisk(75.0));
        assertEquals(RiskCategory.CRITICAL, sviCalculator.categorizeRisk(100.0));
    }
}


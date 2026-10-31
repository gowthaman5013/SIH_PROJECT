package com.nhaa.ai.risk;

import com.nhaa.ai.dto.SafetyContextDto;
import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.TextMlIndicatorsDto;
import com.nhaa.ai.dto.VoiceMlIndicatorsDto;
import com.nhaa.ai.risk.dto.SafetyAssessmentResultDto;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.service.safety.SafetyLayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SafetyLayerServiceTest {

    private SafetyLayerService safetyLayerService;

    @BeforeEach
    void setUp() {
        safetyLayerService = new SafetyLayerService();
    }

    @Test
    @DisplayName("Monotonic Safety Override: LOW SVI + Immediate Danger -> CRITICAL Risk Category")
    void testImmediateDangerOverride() {
        SviResult lowSvi = new SviResult(15.0, 10.0, 20.0, 0.4, 0.3, 0.3, 0.16, 16.0, RiskCategory.LOW);
        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto();
        textMl.setImmediateDanger(0.85);

        StructuredContextDto context = new StructuredContextDto();
        context.getSafetyContext().setImmediateSafetyConcern("TRUE");

        SafetyAssessmentResultDto result = safetyLayerService.evaluateSafety(lowSvi, textMl, new VoiceMlIndicatorsDto(), context);

        assertEquals(RiskCategory.CRITICAL, result.getFinalRiskCategory());
        assertTrue(result.isSafetyEscalated());
        assertTrue(result.getSafetyIndicators().contains("IMMEDIATE_DANGER"));
    }

    @Test
    @DisplayName("Monotonic Safety Override: LOW SVI + Self Harm -> CRITICAL Risk Category")
    void testSelfHarmOverride() {
        SviResult lowSvi = new SviResult(10.0, 10.0, 10.0, 0.4, 0.3, 0.3, 0.10, 10.0, RiskCategory.LOW);
        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto();
        textMl.setSelfHarmSuicidalIdeation(0.90);

        SafetyAssessmentResultDto result = safetyLayerService.evaluateSafety(lowSvi, textMl, new VoiceMlIndicatorsDto(), new StructuredContextDto());

        assertEquals(RiskCategory.CRITICAL, result.getFinalRiskCategory());
        assertTrue(result.isSafetyEscalated());
        assertTrue(result.getSafetyIndicators().contains("SELF_HARM_SUICIDAL_IDEATION"));
    }

    @Test
    @DisplayName("Monotonic Safety Guarantee: High baseline SVI is NEVER reduced by Safety Layer")
    void testMonotonicNonReduction() {
        SviResult highSvi = new SviResult(75.0, 70.0, 80.0, 0.4, 0.3, 0.3, 0.75, 75.0, RiskCategory.HIGH);

        SafetyAssessmentResultDto result = safetyLayerService.evaluateSafety(highSvi, new TextMlIndicatorsDto(), new VoiceMlIndicatorsDto(), new StructuredContextDto());

        assertTrue(result.getFinalRiskCategory().ordinal() >= RiskCategory.HIGH.ordinal());
        assertEquals(RiskCategory.HIGH, result.getFinalRiskCategory());
    }
}

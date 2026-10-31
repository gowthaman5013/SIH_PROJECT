package com.nhaa.ai.risk;

import com.nhaa.ai.dto.IncidentContextDto;
import com.nhaa.ai.dto.SafetyContextDto;
import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.TextMlIndicatorsDto;
import com.nhaa.ai.risk.config.SviConfiguration;
import com.nhaa.ai.risk.dto.SafetyAssessmentResultDto;
import com.nhaa.ai.risk.dto.SupportRecommendationDto;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.service.safety.SafetyLayerService;
import com.nhaa.ai.risk.service.scoring.SviCalculator;
import com.nhaa.ai.risk.service.support.SupportPrioritizationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TextOnlyAssessmentTest {

    private SviCalculator sviCalculator;
    private SafetyLayerService safetyLayerService;
    private SupportPrioritizationEngine supportEngine;

    @BeforeEach
    void setUp() {
        SviConfiguration config = new SviConfiguration();
        config.setTextOnlyTextWeight(0.70);
        config.setTextOnlyContextWeight(0.30);
        sviCalculator = new SviCalculator(config);
        supportEngine = new SupportPrioritizationEngine();
        safetyLayerService = new SafetyLayerService(supportEngine);
    }

    @Test
    @DisplayName("Text-Only SVI Formula: SVI = (Text 62 * 0.70) + (Context 70 * 0.30) = 64.40 HIGH")
    void testTextOnlySviFormulaAndCategory() {
        SviResult svi = sviCalculator.calculateTextOnlySvi(62.0, 70.0);

        assertEquals(64.40, svi.getSviScore());
        assertEquals(RiskCategory.HIGH, svi.getRiskCategory());
        assertEquals(0.70, svi.getTextWeight());
        assertEquals(0.30, svi.getContextWeight());
        assertEquals(0.0, svi.getVoiceWeight());
    }

    @Test
    @DisplayName("Categorization: Low (15), Moderate (35), High (60), Critical (80)")
    void testRiskCategories() {
        assertEquals(RiskCategory.LOW, sviCalculator.categorizeRisk(15.0));
        assertEquals(RiskCategory.MODERATE, sviCalculator.categorizeRisk(35.0));
        assertEquals(RiskCategory.HIGH, sviCalculator.categorizeRisk(60.0));
        assertEquals(RiskCategory.CRITICAL, sviCalculator.categorizeRisk(80.0));
    }

    @Test
    @DisplayName("Safety Override: LOW numerical SVI + Immediate Danger -> Human Review & Priority Alert")
    void testSafetyOverridePreservesNumericalSvi() {
        SviResult lowSvi = sviCalculator.calculateTextOnlySvi(10.0, 20.0); // SVI = 13.0 (LOW)
        assertEquals(13.0, lowSvi.getSviScore());
        assertEquals(RiskCategory.LOW, lowSvi.getRiskCategory());

        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto();
        textMl.setImmediateDanger(0.85);

        StructuredContextDto context = new StructuredContextDto();
        context.getSafetyContext().setImmediateSafetyConcern("TRUE");

        SafetyAssessmentResultDto result = safetyLayerService.evaluateSafety(lowSvi, textMl, null, context);

        // Numerical SVI remains 13.0
        assertEquals(13.0, result.getSviValue());
        // Safety Override triggers escalation & alerts
        assertEquals(RiskCategory.CRITICAL, result.getFinalRiskCategory());
        assertTrue(result.getHumanReviewRequired());
        assertTrue(result.getPriorityAlert());
        assertNotNull(result.getOverrideReason());
        assertTrue(result.getOverrideReason().contains("Critical immediate safety concern"));
    }

    @Test
    @DisplayName("Multiple Support Recommendations: Counselling, Legal Aid, Medical, Police, Witness Protection, Emergency")
    void testMultipleSupportRecommendations() {
        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto();
        textMl.setImmediateDanger(0.80);
        textMl.setViolence(0.75);
        textMl.setThreatIntimidation(0.70);
        textMl.setLegalDistress(0.65);
        textMl.setDepressionRelated(0.60);

        StructuredContextDto context = new StructuredContextDto();
        context.getSafetyContext().setImmediateSafetyConcern("TRUE");
        context.getSafetyContext().setCurrentThreat("TRUE");
        context.getSafetyContext().setRetaliationConcern("TRUE");
        context.getSafetyContext().setWitnessStatus("TRUE");
        context.getIncidentContext().setPhysicalViolence("TRUE");
        context.getLegalContext().setLegalHelpRequested(true);

        SviResult svi = sviCalculator.calculateTextOnlySvi(72.4, 78.0); // 74.08 (HIGH)
        SafetyAssessmentResultDto result = safetyLayerService.evaluateSafety(svi, textMl, null, context);

        List<SupportRecommendationDto> recs = result.getSupportPrioritization().getRecommendations();
        assertNotNull(recs);
        assertTrue(recs.size() >= 4);

        boolean hasEmergency = recs.stream().anyMatch(r -> "EMERGENCY_SUPPORT".equals(r.getSupport()));
        boolean hasPolice = recs.stream().anyMatch(r -> "POLICE_INTERVENTION".equals(r.getSupport()));
        boolean hasMedical = recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport()));
        boolean hasLegal = recs.stream().anyMatch(r -> "LEGAL_AID".equals(r.getSupport()));
        boolean hasCounselling = recs.stream().anyMatch(r -> "COUNSELLING".equals(r.getSupport()));
        boolean hasWitness = recs.stream().anyMatch(r -> "WITNESS_PROTECTION_CONSIDERATION".equals(r.getSupport()));

        assertTrue(hasEmergency);
        assertTrue(hasPolice);
        assertTrue(hasMedical);
        assertTrue(hasLegal);
        assertTrue(hasCounselling);
        assertTrue(hasWitness);
    }
}

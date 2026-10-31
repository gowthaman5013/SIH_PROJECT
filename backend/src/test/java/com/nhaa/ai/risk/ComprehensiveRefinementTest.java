package com.nhaa.ai.risk;

import com.nhaa.ai.dto.IncidentContextDto;
import com.nhaa.ai.dto.LegalContextDto;
import com.nhaa.ai.dto.SafetyContextDto;
import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.SupportContextDto;
import com.nhaa.ai.dto.TextMlIndicatorsDto;
import com.nhaa.ai.risk.config.SviConfiguration;
import com.nhaa.ai.risk.dto.SafetyAssessmentResultDto;
import com.nhaa.ai.risk.dto.SupportRecommendationDto;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.service.safety.SafetyLayerService;
import com.nhaa.ai.risk.service.scoring.ContextScoringEngine;
import com.nhaa.ai.risk.service.scoring.SviCalculator;
import com.nhaa.ai.risk.service.scoring.TextScoringEngine;
import com.nhaa.ai.risk.service.support.SupportPrioritizationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComprehensiveRefinementTest {

    private SviCalculator sviCalculator;
    private SafetyLayerService safetyLayerService;
    private SupportPrioritizationEngine supportEngine;
    private TextScoringEngine textScoringEngine;
    private ContextScoringEngine contextScoringEngine;

    @BeforeEach
    void setUp() {
        SviConfiguration config = new SviConfiguration();
        sviCalculator = new SviCalculator(config);
        supportEngine = new SupportPrioritizationEngine();
        safetyLayerService = new SafetyLayerService(supportEngine);
        textScoringEngine = new TextScoringEngine();
        contextScoringEngine = new ContextScoringEngine();
    }

    @Test
    void test1_NormalDisagreement_NoSafetyOverride() {
        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto(0.05, 0.10, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05);
        StructuredContextDto context = new StructuredContextDto();
        SviResult svi = sviCalculator.calculateTextOnlySvi(10.0, 10.0);

        SafetyAssessmentResultDto safety = safetyLayerService.evaluateSafety(svi, textMl, null, context);

        assertFalse(safety.isSafetyEscalated());
        assertEquals(RiskCategory.LOW, safety.getFinalRiskCategory());
    }

    @Test
    void test2_LegalAssistanceRequested_LegalAid() {
        StructuredContextDto context = new StructuredContextDto();
        LegalContextDto legal = new LegalContextDto();
        legal.setLegalHelpRequested(true);
        context.setLegalContext(legal);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(20.0, new TextMlIndicatorsDto(), null, context, List.of(), RiskCategory.LOW);

        boolean hasLegal = recs.stream().anyMatch(r -> "LEGAL_AID".equals(r.getSupport()));
        assertTrue(hasLegal);
    }

    @Test
    void test3_PhysicalInjuryReported_MedicalAssistance() {
        StructuredContextDto context = new StructuredContextDto();
        IncidentContextDto incident = new IncidentContextDto();
        incident.setPhysicalViolence(true);
        context.setIncidentContext(incident);

        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto(0.80, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10);
        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(40.0, textMl, null, context, List.of(), RiskCategory.MODERATE);

        boolean hasMedical = recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport()));
        assertTrue(hasMedical);
    }

    @Test
    void test4_ActiveThreat_PoliceIntervention() {
        StructuredContextDto context = new StructuredContextDto();
        SafetyContextDto safetyContext = new SafetyContextDto();
        safetyContext.setCurrentThreat(true);
        context.setSafetyContext(safetyContext);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(35.0, new TextMlIndicatorsDto(), null, context, List.of("CURRENT_ACTIVE_THREAT"), RiskCategory.MODERATE);

        boolean hasPolice = recs.stream().anyMatch(r -> "POLICE_INTERVENTION".equals(r.getSupport()));
        assertTrue(hasPolice);
    }

    @Test
    void test5_ImmediateDanger_EmergencyAndPoliceAndHumanReview() {
        StructuredContextDto context = new StructuredContextDto();
        SafetyContextDto safetyContext = new SafetyContextDto();
        safetyContext.setImmediateSafetyConcern(true);
        context.setSafetyContext(safetyContext);

        SviResult svi = sviCalculator.calculateTextOnlySvi(23.6, 35.0); // 27.02 MODERATE
        SafetyAssessmentResultDto safety = safetyLayerService.evaluateSafety(svi, new TextMlIndicatorsDto(), null, context);

        assertTrue(safety.getHumanReviewRequired());
        assertTrue(safety.getPriorityAlert());
        // Verify numerical SVI remains 27.02 MODERATE
        assertEquals(27.02, svi.getSviScore());
        assertEquals(RiskCategory.MODERATE, svi.getRiskCategory());
    }

    @Test
    void test6_WitnessAndRetaliation_WitnessProtection() {
        StructuredContextDto context = new StructuredContextDto();
        SafetyContextDto safetyContext = new SafetyContextDto();
        safetyContext.setWitnessStatus(true);
        safetyContext.setRetaliationConcern(true);
        context.setSafetyContext(safetyContext);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(45.0, new TextMlIndicatorsDto(), null, context, List.of(), RiskCategory.MODERATE);

        boolean hasWitness = recs.stream().anyMatch(r -> "WITNESS_PROTECTION_CONSIDERATION".equals(r.getSupport()));
        assertTrue(hasWitness);
    }

    @Test
    void test7_EmotionalDistress_PsychosocialCounselling() {
        StructuredContextDto context = new StructuredContextDto();
        SupportContextDto support = new SupportContextDto();
        support.setUrgentHelpRequested(true);
        context.setSupportContext(support);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(30.0, new TextMlIndicatorsDto(), null, context, List.of(), RiskCategory.MODERATE);

        boolean hasCounselling = recs.stream().anyMatch(r -> "COUNSELLING".equals(r.getSupport()));
        assertTrue(hasCounselling);
    }

    @Test
    void test8_ThreateningToHurtMe_NoMedicalAssistance() {
        // "He is threatening to hurt me" -> threat_intimidation HIGH, physical_violence LOW
        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto(0.05, 0.88, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10);
        StructuredContextDto context = new StructuredContextDto();
        SafetyContextDto safetyContext = new SafetyContextDto();
        safetyContext.setCurrentThreat(true);
        context.setSafetyContext(safetyContext);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(30.0, textMl, null, context, List.of("CURRENT_ACTIVE_THREAT"), RiskCategory.MODERATE);

        boolean hasMedical = recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport()));
        assertFalse(hasMedical, "Medical assistance must NOT be triggered merely for verbal threats to hurt.");
    }

    @Test
    void test9_HeHitMeArmSwollen_TriggersMedicalAssistance() {
        // "He hit me yesterday and my arm is swollen" -> physical_violence HIGH
        TextMlIndicatorsDto textMl = new TextMlIndicatorsDto(0.92, 0.20, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10);
        StructuredContextDto context = new StructuredContextDto();
        IncidentContextDto incident = new IncidentContextDto();
        incident.setPhysicalViolence(true);
        context.setIncidentContext(incident);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(55.0, textMl, null, context, List.of(), RiskCategory.HIGH);

        boolean hasMedical = recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport()));
        assertTrue(hasMedical, "Actual physical assault report MUST trigger Medical Assistance.");
    }

    @Test
    void test10_TextOnlySviCalculation_ExactMatch() {
        // Text ML = 23.6, Context = 35.0
        // Expected = 23.6 * 0.70 + 35.0 * 0.30 = 16.52 + 10.50 = 27.02
        SviResult result = sviCalculator.calculateTextOnlySvi(23.6, 35.0);

        assertEquals(27.02, result.getSviScore(), 0.001);
        assertEquals(RiskCategory.MODERATE, result.getRiskCategory());
    }

    @Test
    void test11_Case1_ParkingArgument_SafeCase_NoOverride() {
        // Case 1 input: argument with neighbor, safe, resolved, no threat or hurt
        String narrative = "Officer: Can you tell me what happened?\n" +
                "Victim: I had an argument with my neighbor about parking.\n" +
                "Officer: Did they threaten or physically hurt you?\n" +
                "Victim: No. We only argued.\n" +
                "Officer: Are you safe right now?\n" +
                "Victim: Yes, I am safe.\n" +
                "Officer: Do you need any immediate assistance?\n" +
                "Victim: No. We talked about it later and resolved the issue.";

        com.nhaa.ai.service.StructuredContextService contextService = new com.nhaa.ai.service.StructuredContextService();
        StructuredContextDto context = contextService.extractStructuredContext(narrative);

        assertEquals("FALSE", context.getSafetyContext().getImmediateSafetyConcern());
        assertEquals("FALSE", context.getSafetyContext().getCurrentThreat());
        assertEquals("FALSE", context.getIncidentContext().getPhysicalViolence());
        assertFalse(context.getSupportContext().getUrgentHelpRequested());

        com.nhaa.ai.service.TextMlClientService textMlClient = new com.nhaa.ai.service.TextMlClientService();
        TextMlIndicatorsDto textMl = textMlClient.analyzeTextNarrative(narrative);

        double textScore = textScoringEngine.calculateTextRiskScore(textMl);
        double contextScore = contextScoringEngine.calculateContextRisk(context);

        SviResult svi = sviCalculator.calculateTextOnlySvi(textScore, contextScore);
        SafetyAssessmentResultDto safety = safetyLayerService.evaluateSafety(svi, textMl, null, context);

        assertEquals(RiskCategory.LOW, svi.getRiskCategory());
        assertFalse(safety.isSafetyEscalated(), "Case 1 must NOT trigger Safety Override.");
        assertFalse(safety.getHumanReviewRequired(), "Case 1 must NOT require mandatory human review priority.");
        assertFalse(safety.getPriorityAlert(), "Case 1 must NOT raise a Priority Alert.");
        assertTrue(safety.getSupportPrioritization().getRecommendations().isEmpty(), "Case 1 must NOT generate urgent support recommendations.");
    }

    @Test
    void test12_RegressionTestA_ThreatenedToHurtNoPhysicalAttack() {
        String narrative = "He threatened to hurt me if I complained. He did not physically attack me. I have no injury.";
        com.nhaa.ai.service.StructuredContextService contextService = new com.nhaa.ai.service.StructuredContextService();
        StructuredContextDto context = contextService.extractStructuredContext(narrative);

        com.nhaa.ai.service.TextMlClientService textMlClient = new com.nhaa.ai.service.TextMlClientService();
        TextMlIndicatorsDto textMl = textMlClient.analyzeTextNarrative(narrative);

        assertTrue(textMl.getProbability("threat_intimidation") >= 0.70, "threat_intimidation must be high for threat to hurt");
        assertTrue(textMl.getProbability("physical_violence") < 0.50, "physical_violence must be low when no attack occurred");

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(30.0, textMl, null, context, List.of(), RiskCategory.MODERATE);
        boolean hasMedical = recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport()));
        assertFalse(hasMedical, "Medical Assistance must NOT be recommended when no physical attack/injury occurred.");
    }

    @Test
    void test13_RegressionTestB_HeHitMeArmSwollen_TriggersMedical() {
        String narrative = "He hit me yesterday. My arm is swollen and painful. I need medical help.";
        com.nhaa.ai.service.StructuredContextService contextService = new com.nhaa.ai.service.StructuredContextService();
        StructuredContextDto context = contextService.extractStructuredContext(narrative);

        assertEquals("TRUE", context.getIncidentContext().getPhysicalViolence());
        assertTrue(context.getSupportContext().getMedicalAssistanceRequested());

        com.nhaa.ai.service.TextMlClientService textMlClient = new com.nhaa.ai.service.TextMlClientService();
        TextMlIndicatorsDto textMl = textMlClient.analyzeTextNarrative(narrative);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(50.0, textMl, null, context, List.of(), RiskCategory.HIGH);
        boolean hasMedical = recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport()));
        assertTrue(hasMedical, "Physical assault + swollen arm + medical request MUST trigger Medical Assistance.");
    }

    @Test
    void test14_RegressionTestC_NoHeIsOutsideThreateningMe_CriticalOverride() {
        String narrative = "Officer: Are you safe right now?\n" +
                "Victim: No, he is outside my house right now and is threatening me.";

        com.nhaa.ai.service.StructuredContextService contextService = new com.nhaa.ai.service.StructuredContextService();
        StructuredContextDto context = contextService.extractStructuredContext(narrative);

        assertEquals("TRUE", context.getSafetyContext().getImmediateSafetyConcern());
        assertEquals("TRUE", context.getSafetyContext().getCurrentThreat());
        assertEquals("TRUE", context.getSafetyContext().getPerpetratorPresent());

        com.nhaa.ai.service.TextMlClientService textMlClient = new com.nhaa.ai.service.TextMlClientService();
        TextMlIndicatorsDto textMl = textMlClient.analyzeTextNarrative(narrative);

        SviResult svi = sviCalculator.calculateTextOnlySvi(40.0, 50.0);
        SafetyAssessmentResultDto safety = safetyLayerService.evaluateSafety(svi, textMl, null, context);

        assertTrue(safety.isSafetyEscalated(), "Immediate danger outside house MUST trigger Safety Override.");
        assertEquals(RiskCategory.CRITICAL, safety.getFinalRiskCategory());
        assertTrue(safety.getHumanReviewRequired());
        assertTrue(safety.getPriorityAlert());

        List<SupportRecommendationDto> recs = safety.getSupportPrioritization().getRecommendations();
        assertTrue(recs.stream().anyMatch(r -> "EMERGENCY_SUPPORT".equals(r.getSupport())));
        assertTrue(recs.stream().anyMatch(r -> "POLICE_INTERVENTION".equals(r.getSupport())));
    }

    @Test
    void test15_RegressionTestD_HistoricalThreatSafeWantLegalAdvice() {
        String narrative = "He threatened me last month. He is not contacting me now. I am safe and I only want legal advice.";
        com.nhaa.ai.service.StructuredContextService contextService = new com.nhaa.ai.service.StructuredContextService();
        StructuredContextDto context = contextService.extractStructuredContext(narrative);

        assertEquals("FALSE", context.getSafetyContext().getImmediateSafetyConcern());
        assertTrue(context.getLegalContext().getLegalHelpRequested());

        com.nhaa.ai.service.TextMlClientService textMlClient = new com.nhaa.ai.service.TextMlClientService();
        TextMlIndicatorsDto textMl = textMlClient.analyzeTextNarrative(narrative);

        List<SupportRecommendationDto> recs = supportEngine.generateRecommendationsList(25.0, textMl, null, context, List.of(), RiskCategory.MODERATE);

        assertTrue(recs.stream().anyMatch(r -> "LEGAL_AID".equals(r.getSupport())));
        assertFalse(recs.stream().anyMatch(r -> "EMERGENCY_SUPPORT".equals(r.getSupport())));
        assertFalse(recs.stream().anyMatch(r -> "MEDICAL_ASSISTANCE".equals(r.getSupport())));
    }
}

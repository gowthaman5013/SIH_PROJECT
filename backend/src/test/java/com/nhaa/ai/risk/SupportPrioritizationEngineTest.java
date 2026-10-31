package com.nhaa.ai.risk;

import com.nhaa.ai.dto.IncidentContextDto;
import com.nhaa.ai.dto.LegalContextDto;
import com.nhaa.ai.dto.PropertyContextDto;
import com.nhaa.ai.dto.SafetyContextDto;
import com.nhaa.ai.dto.SocialContextDto;
import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.SupportContextDto;
import com.nhaa.ai.dto.TextMlIndicatorsDto;
import com.nhaa.ai.dto.VoiceMlIndicatorsDto;
import com.nhaa.ai.risk.dto.SupportDebugTraceDto;
import com.nhaa.ai.risk.dto.SupportPrioritizationResultDto;
import com.nhaa.ai.risk.dto.SupportRecommendationDto;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.model.SafetyPriority;
import com.nhaa.ai.risk.service.support.SupportPrioritizationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupportPrioritizationEngineTest {

    private SupportPrioritizationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SupportPrioritizationEngine();
    }

    private TextMlIndicatorsDto createNeutralText() {
        return new TextMlIndicatorsDto(0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05, 0.05);
    }

    private VoiceMlIndicatorsDto createNeutralVoice() {
        return new VoiceMlIndicatorsDto(0.1, 0.1, 0.1, 0.1, 0.1);
    }

    private StructuredContextDto createContext() {
        return new StructuredContextDto();
    }

    private boolean hasRecommendation(SupportPrioritizationResultDto result, String supportName) {
        return result.getRecommendations().stream().anyMatch(r -> supportName.equals(r.getSupport()));
    }

    @Test
    @DisplayName("TC1 — Normal / Low Risk: No threats, no injury, safe -> All domains NOT REQUIRED, debug trace safe")
    void testTC1_NormalLowRisk() {
        SupportPrioritizationResultDto result = engine.prioritize(
                12.0, RiskCategory.LOW, RiskCategory.LOW,
                createNeutralText(), createNeutralVoice(), createContext(),
                List.of(), SafetyPriority.ROUTINE
        );

        assertNotNull(result);
        assertEquals("NOT_REQUIRED", result.getCounsellingReview().getStatus());
        assertEquals("NOT_REQUIRED", result.getRehabilitationReview().getStatus());
        assertEquals("NOT_REQUIRED", result.getLegalAidReview().getStatus());
        assertEquals("NOT_REQUIRED", result.getMedicalSupportReview().getStatus());
        assertEquals("STANDARD", result.getProtectionEmergencyReview().getStatus());
        assertTrue(result.getRecommendations().isEmpty());

        SupportDebugTraceDto trace = result.getDebugTrace();
        assertNotNull(trace);
        assertFalse(trace.getHumanReview());
        assertFalse(trace.getSafetyOverride());
    }

    @Test
    @DisplayName("TC2 — Legal Aid: Low SVI must NOT suppress Legal Aid recommendation")
    void testTC2_LegalAid_LowSvi() {
        StructuredContextDto ctx = createContext();
        LegalContextDto legal = new LegalContextDto();
        legal.setLegalHelpRequested(true);
        legal.setPoliceComplaintFiled(true);
        ctx.setLegalContext(legal);

        SupportPrioritizationResultDto result = engine.prioritize(
                15.0, RiskCategory.LOW, RiskCategory.LOW,
                createNeutralText(), createNeutralVoice(), ctx,
                List.of(), SafetyPriority.ROUTINE
        );

        assertEquals("HIGH_PRIORITY", result.getLegalAidReview().getStatus());
        assertTrue(hasRecommendation(result, "LEGAL_AID"));
        assertFalse(hasRecommendation(result, "EMERGENCY_SUPPORT"));
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
        assertFalse(result.getDebugTrace().getHumanReview());
    }

    @Test
    @DisplayName("TC3 — Physical Violence + Medical: Physical injury reported triggers Medical Assistance")
    void testTC3_PhysicalViolence_Medical() {
        StructuredContextDto ctx = createContext();
        IncidentContextDto incident = new IncidentContextDto();
        incident.setPhysicalViolence(true);
        incident.setIncidentDescription("He hit me yesterday and my arm is injured.");
        ctx.setIncidentContext(incident);
        TextMlIndicatorsDto text = createNeutralText();
        text.setPhysicalViolence(0.85);

        SupportPrioritizationResultDto result = engine.prioritize(
                45.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                text, createNeutralVoice(), ctx,
                List.of(), SafetyPriority.PRIORITY
        );

        assertEquals("REQUIRED", result.getMedicalSupportReview().getStatus());
        assertTrue(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertFalse(hasRecommendation(result, "EMERGENCY_SUPPORT"));
    }

    @Test
    @DisplayName("TC4 — Active Threat: Threat intimidation high triggers Police Intervention, NOT Medical")
    void testTC4_ActiveThreat() {
        StructuredContextDto ctx = createContext();
        SafetyContextDto safety = new SafetyContextDto();
        safety.setCurrentThreat(true);
        ctx.setSafetyContext(safety);

        TextMlIndicatorsDto text = createNeutralText();
        text.setThreatIntimidation(0.85);

        SupportPrioritizationResultDto result = engine.prioritize(
                35.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                text, createNeutralVoice(), ctx,
                List.of("CURRENT_ACTIVE_THREAT"), SafetyPriority.PRIORITY
        );

        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
    }

    @Test
    @DisplayName("TC5 — Immediate Danger: Perpetrator present + active threat -> Emergency URGENT, Police URGENT, Safety Override")
    void testTC5_ImmediateDanger() {
        StructuredContextDto ctx = createContext();
        SafetyContextDto safety = new SafetyContextDto();
        safety.setImmediateSafetyConcern(true);
        safety.setPerpetratorPresent(true);
        safety.setCurrentThreat(true);
        ctx.setSafetyContext(safety);

        IncidentContextDto incident = new IncidentContextDto();
        incident.setIncidentDescription("He is outside threatening to hurt me.");
        ctx.setIncidentContext(incident);

        SupportPrioritizationResultDto result = engine.prioritize(
                65.0, RiskCategory.HIGH, RiskCategory.CRITICAL,
                createNeutralText(), createNeutralVoice(), ctx,
                List.of("IMMEDIATE_DANGER", "CURRENT_ACTIVE_THREAT"), SafetyPriority.IMMEDIATE
        );

        assertEquals("IMMEDIATE_EMERGENCY", result.getProtectionEmergencyReview().getStatus());
        assertTrue(hasRecommendation(result, "EMERGENCY_SUPPORT"));
        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"), "Threat to hurt is NOT physical injury.");

        SupportDebugTraceDto trace = result.getDebugTrace();
        assertTrue(trace.getHumanReview());
        assertTrue(trace.getSafetyOverride());
    }

    @Test
    @DisplayName("TC6 — Weapon / Life Threat: High weapon life threat -> Police URGENT, Emergency URGENT, Human Review YES")
    void testTC6_WeaponLifeThreat() {
        TextMlIndicatorsDto text = createNeutralText();
        text.setWeaponLifeThreat(0.80);
        text.setThreatIntimidation(0.75);

        SupportPrioritizationResultDto result = engine.prioritize(
                60.0, RiskCategory.HIGH, RiskCategory.CRITICAL,
                text, createNeutralVoice(), createContext(),
                List.of("SEVERE_THREAT_INTIMIDATION"), SafetyPriority.IMMEDIATE
        );

        assertTrue(hasRecommendation(result, "EMERGENCY_SUPPORT"));
        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
        assertTrue(result.getDebugTrace().getHumanReview());
    }

    @Test
    @DisplayName("TC7 — Family Threat: Threat against family -> Police Intervention HIGH, Emergency NOT automatically triggered")
    void testTC7_FamilyThreat() {
        TextMlIndicatorsDto text = createNeutralText();
        text.setFamilyAbuse(0.75);
        text.setThreatIntimidation(0.70);

        SupportPrioritizationResultDto result = engine.prioritize(
                40.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                text, createNeutralVoice(), createContext(),
                List.of(), SafetyPriority.PRIORITY
        );

        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertFalse(hasRecommendation(result, "EMERGENCY_SUPPORT"));
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
    }

    @Test
    @DisplayName("TC8 — Social Isolation + Displacement: Housing loss/displacement -> Rehabilitation REQUIRED, Emergency NOT REQUIRED")
    void testTC8_SocialIsolationDisplacement() {
        StructuredContextDto ctx = createContext();
        SocialContextDto social = new SocialContextDto();
        social.setDisplacement(true);
        ctx.setSocialContext(social);

        TextMlIndicatorsDto text = createNeutralText();
        text.setPropertyDispossession(0.75);

        SupportPrioritizationResultDto result = engine.prioritize(
                30.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                text, createNeutralVoice(), ctx,
                List.of(), SafetyPriority.ROUTINE
        );

        assertEquals("REQUIRED", result.getRehabilitationReview().getStatus());
        assertTrue(hasRecommendation(result, "REHABILITATION"));
        assertFalse(hasRecommendation(result, "EMERGENCY_SUPPORT"));
    }

    @Test
    @DisplayName("TC9 — Witness Protection: Key witness + retaliation -> WITNESS_PROTECTION_CONSIDERATION, Human Review YES")
    void testTC9_WitnessProtection() {
        StructuredContextDto ctx = createContext();
        SafetyContextDto safety = new SafetyContextDto();
        safety.setWitnessStatus(true);
        safety.setRetaliationConcern(true);
        ctx.setSafetyContext(safety);

        SupportPrioritizationResultDto result = engine.prioritize(
                45.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                createNeutralText(), createNeutralVoice(), ctx,
                List.of(), SafetyPriority.PRIORITY
        );

        assertTrue(hasRecommendation(result, "WITNESS_PROTECTION_CONSIDERATION"));
        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertTrue(result.getDebugTrace().getHumanReview());
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
    }

    @Test
    @DisplayName("TC10 — Psychosocial Counselling: Emotional distress -> Counselling HIGH with neutral explanation")
    void testTC10_PsychosocialCounselling() {
        StructuredContextDto ctx = createContext();
        SupportContextDto support = new SupportContextDto();
        support.setUrgentHelpRequested(true);
        ctx.setSupportContext(support);

        TextMlIndicatorsDto text = createNeutralText();
        text.setStalkingCyberHarassment(0.75);

        SupportPrioritizationResultDto result = engine.prioritize(
                35.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                text, createNeutralVoice(), ctx,
                List.of(), SafetyPriority.ROUTINE
        );

        assertEquals("HIGH_PRIORITY", result.getCounsellingReview().getStatus());
        assertTrue(hasRecommendation(result, "COUNSELLING"));

        SupportRecommendationDto rec = result.getRecommendations().stream()
                .filter(r -> "COUNSELLING".equals(r.getSupport())).findFirst().orElseThrow();
        assertTrue(rec.getReason().contains("Psychosocial support is recommended based on reported distress."));
    }

    @Test
    @DisplayName("TC11 — Repeat Threats: Repeat offence -> Police Intervention HIGH")
    void testTC11_RepeatThreats() {
        TextMlIndicatorsDto text = createNeutralText();
        text.setRepeatOffence(0.80);
        text.setThreatIntimidation(0.75);

        SupportPrioritizationResultDto result = engine.prioritize(
                50.0, RiskCategory.HIGH, RiskCategory.HIGH,
                text, createNeutralVoice(), createContext(),
                List.of(), SafetyPriority.PRIORITY
        );

        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertFalse(hasRecommendation(result, "EMERGENCY_SUPPORT"));
    }

    @Test
    @DisplayName("TC12 — Multiple Indicators: Co-existence of Medical HIGH, Police HIGH, Legal HIGH, Counselling HIGH")
    void testTC12_MultipleIndicators() {
        StructuredContextDto ctx = createContext();
        IncidentContextDto incident = new IncidentContextDto();
        incident.setPhysicalViolence(true);
        incident.setIncidentDescription("Victim was attacked and has swelling.");
        ctx.setIncidentContext(incident);

        LegalContextDto legal = new LegalContextDto();
        legal.setLegalHelpRequested(true);
        ctx.setLegalContext(legal);

        SupportContextDto support = new SupportContextDto();
        support.setUrgentHelpRequested(true);
        ctx.setSupportContext(support);

        TextMlIndicatorsDto text = createNeutralText();
        text.setPhysicalViolence(0.80);
        text.setThreatIntimidation(0.75);
        text.setFamilyAbuse(0.65);

        SupportPrioritizationResultDto result = engine.prioritize(
                65.0, RiskCategory.HIGH, RiskCategory.HIGH,
                text, createNeutralVoice(), ctx,
                List.of(), SafetyPriority.PRIORITY
        );

        assertTrue(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
        assertTrue(hasRecommendation(result, "LEGAL_AID"));
        assertTrue(hasRecommendation(result, "COUNSELLING"));
    }

    @Test
    @DisplayName("Test A — False Positive Test: Verbal threat 'threatened to hurt me' does NOT trigger Medical Assistance")
    void testTestA_ThreatenedToHurt_NoMedical() {
        StructuredContextDto ctx = createContext();
        IncidentContextDto incident = new IncidentContextDto();
        incident.setIncidentDescription("He threatened to hurt me if I complained. He did not physically attack me. I have no injury.");
        ctx.setIncidentContext(incident);

        TextMlIndicatorsDto text = createNeutralText();
        text.setThreatIntimidation(0.85);

        SupportPrioritizationResultDto result = engine.prioritize(
                30.0, RiskCategory.MODERATE, RiskCategory.MODERATE,
                text, createNeutralVoice(), ctx,
                List.of("CURRENT_ACTIVE_THREAT"), SafetyPriority.PRIORITY
        );

        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"), "Medical Assistance must NOT be triggered for verbal threats without physical attack.");
        assertTrue(hasRecommendation(result, "POLICE_INTERVENTION"));
    }

    @Test
    @DisplayName("Test B — Actual Injury: 'hit me yesterday, arm swollen, need medical help' MUST trigger Medical Assistance HIGH")
    void testTestB_ActualInjury_MedicalTriggered() {
        StructuredContextDto ctx = createContext();
        IncidentContextDto incident = new IncidentContextDto();
        incident.setPhysicalViolence(true);
        incident.setIncidentDescription("He hit me yesterday. My arm is swollen and painful.");
        ctx.setIncidentContext(incident);

        SupportContextDto support = new SupportContextDto();
        support.setMedicalAssistanceRequested(true);
        ctx.setSupportContext(support);

        TextMlIndicatorsDto text = createNeutralText();
        text.setPhysicalViolence(0.90);

        SupportPrioritizationResultDto result = engine.prioritize(
                55.0, RiskCategory.HIGH, RiskCategory.HIGH,
                text, createNeutralVoice(), ctx,
                List.of(), SafetyPriority.PRIORITY
        );

        assertTrue(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
        assertEquals("REQUIRED", result.getMedicalSupportReview().getStatus());
    }

    @Test
    @DisplayName("Test C — Past Incident, Safe Now: 'attacked last month, safe now, want legal advice' triggers Legal Aid, NOT Emergency")
    void testTestC_PastIncidentSafe_LegalAid() {
        StructuredContextDto ctx = createContext();
        LegalContextDto legal = new LegalContextDto();
        legal.setLegalHelpRequested(true);
        ctx.setLegalContext(legal);

        SupportPrioritizationResultDto result = engine.prioritize(
                25.0, RiskCategory.LOW, RiskCategory.LOW,
                createNeutralText(), createNeutralVoice(), ctx,
                List.of(), SafetyPriority.ROUTINE
        );

        assertTrue(hasRecommendation(result, "LEGAL_AID"));
        assertFalse(hasRecommendation(result, "EMERGENCY_SUPPORT"));
        assertFalse(hasRecommendation(result, "MEDICAL_ASSISTANCE"));
    }
}

package com.nhaa.ai.risk.service.support;

import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.TextMlIndicatorsDto;
import com.nhaa.ai.dto.VoiceMlIndicatorsDto;
import com.nhaa.ai.risk.dto.SupportDebugTraceDto;
import com.nhaa.ai.risk.dto.SupportPrioritizationResultDto;
import com.nhaa.ai.risk.dto.SupportRecommendationDto;
import com.nhaa.ai.risk.dto.SupportReviewItemDto;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.model.SafetyPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Deterministic Evidence-Based Support Prioritization Engine.
 * 
 * Maps recommendations across explicit support domains:
 * 1. EMERGENCY_SUPPORT
 * 2. POLICE_INTERVENTION
 * 3. MEDICAL_ASSISTANCE
 * 4. LEGAL_AID
 * 5. COUNSELLING (Psychosocial Counselling)
 * 6. WITNESS_PROTECTION_CONSIDERATION
 * 7. REHABILITATION (Shelter & Social Support)
 * 
 * Driven independently by domain-specific empirical evidence:
 * - Text ML indicators
 * - Voice ML distress signals
 * - Structured context flags (safety, incident, legal, support, property, social)
 * - Explicit safety conditions
 * 
 * Decoupled from SVI numerical score and Risk Category as sole triggers.
 */
@Service
public class SupportPrioritizationEngine {

    private static final Logger logger = LoggerFactory.getLogger(SupportPrioritizationEngine.class);

    public SupportPrioritizationResultDto prioritize(
            Double sviValue,
            RiskCategory initialRiskCategory,
            RiskCategory finalRiskCategory,
            TextMlIndicatorsDto textMl,
            VoiceMlIndicatorsDto voiceMl,
            StructuredContextDto context,
            List<String> safetyIndicators,
            SafetyPriority safetyPriority
    ) {
        double svi = (sviValue != null) ? sviValue : 0.0;
        List<String> indicators = (safetyIndicators != null) ? safetyIndicators : Collections.emptyList();
        RiskCategory finalCat = (finalRiskCategory != null) ? finalRiskCategory : RiskCategory.LOW;

        logger.debug("Executing Support Prioritization: SVI={}, finalCat={}, priority={}", svi, finalCat, safetyPriority);

        // 1. Evaluate Signals & Evidence
        boolean immediateDanger = isImmediateDanger(textMl, context, indicators, safetyPriority);
        boolean perpetratorPresent = context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getPerpetratorPresent());
        boolean activeThreat = isActiveThreat(textMl, context, indicators);
        boolean weaponLifeThreat = textMl != null && textMl.getProbability("weapon_life_threat") >= 0.60;
        boolean criticalCategory = finalCat == RiskCategory.CRITICAL;

        boolean emergencyTriggered = immediateDanger || (perpetratorPresent && activeThreat) || weaponLifeThreat || criticalCategory || safetyPriority == SafetyPriority.IMMEDIATE;

        boolean physicalInjuryReported = isPhysicalInjuryReported(context);
        boolean physicalViolenceHigh = textMl != null && textMl.getProbability("physical_violence") >= 0.70;
        boolean medicalHelpRequested = context != null && context.getSupportContext() != null && isTrue(context.getSupportContext().getMedicalAssistanceRequested());
        boolean medicalTriggered = physicalInjuryReported || physicalViolenceHigh || medicalHelpRequested;

        boolean policeProtectionNeed = context != null && context.getSafetyContext() != null && (isTrue(context.getSafetyContext().getProtectionRequested()) || isTrue(context.getSafetyContext().getOngoingThreats()));
        boolean retaliationConcern = context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getRetaliationConcern());
        boolean repeatOffence = textMl != null && textMl.getProbability("repeat_offence") >= 0.70;
        boolean policeTriggered = emergencyTriggered || activeThreat || (textMl != null && textMl.getProbability("threat_intimidation") >= 0.70) || repeatOffence || policeProtectionNeed || retaliationConcern || physicalViolenceHigh || physicalInjuryReported;

        boolean legalHelpRequested = context != null && context.getLegalContext() != null && isTrue(context.getLegalContext().getLegalHelpRequested());
        boolean activeCourtCase = context != null && context.getLegalContext() != null && (isTrue(context.getLegalContext().getCourtCaseExists()) || isTrue(context.getLegalContext().getPoliceComplaintFiled()) || isTrue(context.getLegalContext().getFirFiled()));
        boolean institutionalDenialHigh = textMl != null && textMl.getProbability("institutional_denial") >= 0.70;
        boolean legalTriggered = legalHelpRequested || activeCourtCase || institutionalDenialHigh;

        boolean selfHarm = indicators.contains("SELF_HARM_SUICIDAL_IDEATION") || (textMl != null && textMl.getProbability("self_harm_suicidal_ideation") >= 0.50);
        boolean counsellingRequested = context != null && context.getSupportContext() != null && isTrue(context.getSupportContext().getUrgentHelpRequested());
        boolean emotionalDistress = selfHarm || counsellingRequested
                || (textMl != null && (textMl.getProbability("stalking_cyber_harassment") >= 0.50 || textMl.getProbability("family_abuse") >= 0.50 || textMl.getProbability("depression_related") >= 0.50))
                || (voiceMl != null && voiceMl.getIndicators() != null && voiceMl.getIndicators().getOrDefault("acoustic_distress", 0.0) >= 0.50);
        boolean counsellingTriggered = emotionalDistress;

        boolean witnessStatus = context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getWitnessStatus());
        boolean witnessProtectionTriggered = witnessStatus && (retaliationConcern || activeThreat || immediateDanger || indicators.contains("IMMEDIATE_DANGER"));

        boolean displacement = (context != null && context.getSocialContext() != null && isTrue(context.getSocialContext().getDisplacement()))
                || (context != null && context.getPropertyContext() != null && isTrue(context.getPropertyContext().getPropertyDispossession()))
                || (textMl != null && textMl.getProbability("property_dispossession") >= 0.65);
        boolean rehabilitationTriggered = displacement;

        // 2. Evaluate Domain Reviews
        SupportReviewItemDto protectionEmergency = evaluateProtectionEmergencyItem(emergencyTriggered, activeThreat, policeTriggered);
        SupportReviewItemDto medical = evaluateMedicalItem(medicalTriggered);
        SupportReviewItemDto legalAid = evaluateLegalAidItem(legalTriggered);
        SupportReviewItemDto counselling = evaluateCounsellingItem(selfHarm, counsellingTriggered);
        SupportReviewItemDto rehabilitation = evaluateRehabilitationItem(rehabilitationTriggered);

        // 3. Generate Recommendations List
        List<SupportRecommendationDto> recommendations = generateRecommendationsListInternal(
                emergencyTriggered, policeTriggered, medicalTriggered, legalTriggered, counsellingTriggered, witnessProtectionTriggered, rehabilitationTriggered,
                immediateDanger, activeThreat, weaponLifeThreat, physicalInjuryReported, physicalViolenceHigh, medicalHelpRequested, legalHelpRequested, activeCourtCase,
                selfHarm, witnessStatus, retaliationConcern, displacement, context
        );

        // 4. Construct Debug Trace Object
        SupportDebugTraceDto debugTrace = createDebugTrace(
                svi, finalCat, emergencyTriggered, policeTriggered, medicalTriggered, legalTriggered, counsellingTriggered, witnessProtectionTriggered, rehabilitationTriggered,
                immediateDanger, perpetratorPresent, activeThreat, weaponLifeThreat, physicalInjuryReported, physicalViolenceHigh, medicalHelpRequested, legalHelpRequested,
                activeCourtCase, selfHarm, witnessStatus, retaliationConcern, displacement, protectionEmergency, medical, legalAid, counselling, rehabilitation, recommendations
        );

        return new SupportPrioritizationResultDto(
                counselling,
                rehabilitation,
                legalAid,
                medical,
                protectionEmergency,
                recommendations,
                debugTrace
        );
    }

    public List<SupportRecommendationDto> generateRecommendationsList(
            double svi,
            TextMlIndicatorsDto textMl,
            VoiceMlIndicatorsDto voiceMl,
            StructuredContextDto context,
            List<String> indicators,
            RiskCategory finalCat
    ) {
        SupportPrioritizationResultDto result = prioritize(
                svi, finalCat, finalCat, textMl, voiceMl, context, indicators, SafetyPriority.ROUTINE
        );
        return result.getRecommendations();
    }

    private List<SupportRecommendationDto> generateRecommendationsListInternal(
            boolean emergencyTriggered,
            boolean policeTriggered,
            boolean medicalTriggered,
            boolean legalTriggered,
            boolean counsellingTriggered,
            boolean witnessProtectionTriggered,
            boolean rehabilitationTriggered,
            boolean immediateDanger,
            boolean activeThreat,
            boolean weaponLifeThreat,
            boolean physicalInjuryReported,
            boolean physicalViolenceHigh,
            boolean medicalHelpRequested,
            boolean legalHelpRequested,
            boolean activeCourtCase,
            boolean selfHarm,
            boolean witnessStatus,
            boolean retaliationConcern,
            boolean displacement,
            StructuredContextDto context
    ) {
        List<SupportRecommendationDto> recs = new ArrayList<>();

        // 1. EMERGENCY_SUPPORT
        if (emergencyTriggered) {
            List<String> evidence = new ArrayList<>();
            if (immediateDanger) evidence.add("Immediate safety concern reported or detected in narrative context.");
            if (activeThreat) evidence.add("Active current threat reported.");
            if (context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getPerpetratorPresent())) {
                evidence.add("Perpetrator reported currently present at victim location.");
            }
            if (weaponLifeThreat) evidence.add("Life threat or weapon involvement indicator detected.");
            recs.add(new SupportRecommendationDto("EMERGENCY_SUPPORT", "URGENT", "Immediate danger or active life-safety threat detected; urgent crisis response required.", evidence, true));
        }

        // 2. POLICE_INTERVENTION
        if (policeTriggered) {
            List<String> evidence = new ArrayList<>();
            if (activeThreat) evidence.add("Active intimidation or threat reported.");
            if (immediateDanger) evidence.add("Urgent protection requirement detected.");
            if (retaliationConcern) evidence.add("Retaliation concern reported against victim/witness.");
            String priority = (immediateDanger || activeThreat || weaponLifeThreat) ? "URGENT" : "HIGH";
            boolean humanReview = (immediateDanger || activeThreat || weaponLifeThreat);
            recs.add(new SupportRecommendationDto("POLICE_INTERVENTION", priority, "Police intervention/protection review recommended.", evidence, humanReview));
        }

        // 3. MEDICAL_ASSISTANCE
        if (medicalTriggered) {
            List<String> evidence = new ArrayList<>();
            if (physicalInjuryReported) evidence.add("Victim reported physical assault or physical injury.");
            if (physicalViolenceHigh) evidence.add("High physical violence probability detected.");
            if (medicalHelpRequested) evidence.add("Victim explicitly requested medical assistance.");
            recs.add(new SupportRecommendationDto("MEDICAL_ASSISTANCE", "HIGH", "Reported physical injury or medical request; medical examination and care recommended.", evidence, false));
        }

        // 4. LEGAL_AID
        if (legalTriggered) {
            List<String> evidence = new ArrayList<>();
            if (legalHelpRequested) evidence.add("Victim explicitly requested legal assistance or guidance.");
            if (activeCourtCase) evidence.add("Active court proceeding, FIR, or police complaint reported.");
            recs.add(new SupportRecommendationDto("LEGAL_AID", "HIGH", "Legal aid and procedure guidance recommended.", evidence, false));
        }

        // 5. PSYCHOSOCIAL_COUNSELLING
        if (counsellingTriggered) {
            List<String> evidence = new ArrayList<>();
            if (selfHarm) evidence.add("Self-harm or severe crisis signals observed.");
            else evidence.add("Emotional distress or trauma indicators observed.");
            String priority = selfHarm ? "URGENT" : "HIGH";
            boolean humanReview = selfHarm;
            recs.add(new SupportRecommendationDto("COUNSELLING", priority, "Psychosocial support is recommended based on reported distress.", evidence, humanReview));
        }

        // 6. WITNESS_PROTECTION_CONSIDERATION
        if (witnessProtectionTriggered) {
            List<String> evidence = new ArrayList<>();
            if (witnessStatus) evidence.add("Victim identified as key witness.");
            if (retaliationConcern) evidence.add("Retaliation threat reported against witness.");
            String priority = retaliationConcern ? "URGENT" : "HIGH";
            recs.add(new SupportRecommendationDto("WITNESS_PROTECTION_CONSIDERATION", priority, "Witness protection consideration recommended.", evidence, true));
        }

        // 7. REHABILITATION / SHELTER
        if (rehabilitationTriggered) {
            List<String> evidence = new ArrayList<>();
            if (displacement) evidence.add("Displacement, housing loss, or property dispossession reported.");
            recs.add(new SupportRecommendationDto("REHABILITATION", "HIGH", "Rehabilitation and emergency shelter support recommended.", evidence, false));
        }

        return recs;
    }

    private SupportReviewItemDto evaluateProtectionEmergencyItem(boolean emergencyTriggered, boolean activeThreat, boolean policeTriggered) {
        if (emergencyTriggered) {
            return new SupportReviewItemDto("IMMEDIATE_EMERGENCY", 4, "Immediate safety danger detected; emergency crisis response required.", true);
        }
        if (activeThreat || policeTriggered) {
            return new SupportReviewItemDto("URGENT", 3, "Active threat or high risk detected; protection review required.", true);
        }
        return new SupportReviewItemDto("STANDARD", 1, "Standard protection monitoring.", false);
    }

    private SupportReviewItemDto evaluateMedicalItem(boolean medicalTriggered) {
        if (medicalTriggered) {
            return new SupportReviewItemDto("REQUIRED", 3, "Reported physical injury or physical assault; medical examination required.", false);
        }
        return new SupportReviewItemDto("NOT_REQUIRED", 0, "No physical injury reported.", false);
    }

    private SupportReviewItemDto evaluateLegalAidItem(boolean legalTriggered) {
        if (legalTriggered) {
            return new SupportReviewItemDto("HIGH_PRIORITY", 3, "Legal help requested or active legal proceedings.", false);
        }
        return new SupportReviewItemDto("NOT_REQUIRED", 0, "No legal aid review currently requested.", false);
    }

    private SupportReviewItemDto evaluateCounsellingItem(boolean selfHarm, boolean counsellingTriggered) {
        if (selfHarm) {
            return new SupportReviewItemDto("URGENT", 4, "Severe psychological crisis and self-harm risk indicators detected.", true);
        }
        if (counsellingTriggered) {
            return new SupportReviewItemDto("HIGH_PRIORITY", 3, "Psychosocial support is recommended based on reported distress.", false);
        }
        return new SupportReviewItemDto("NOT_REQUIRED", 0, "No acute psychological distress indicators currently observed.", false);
    }

    private SupportReviewItemDto evaluateRehabilitationItem(boolean rehabilitationTriggered) {
        if (rehabilitationTriggered) {
            return new SupportReviewItemDto("REQUIRED", 3, "Rehabilitation and emergency shelter support required.", false);
        }
        return new SupportReviewItemDto("NOT_REQUIRED", 0, "Rehabilitation support options available.", false);
    }

    private SupportDebugTraceDto createDebugTrace(
            double svi, RiskCategory finalCat,
            boolean emergencyTriggered, boolean policeTriggered, boolean medicalTriggered, boolean legalTriggered, boolean counsellingTriggered, boolean witnessProtectionTriggered, boolean rehabilitationTriggered,
            boolean immediateDanger, boolean perpetratorPresent, boolean activeThreat, boolean weaponLifeThreat, boolean physicalInjuryReported, boolean physicalViolenceHigh, boolean medicalHelpRequested, boolean legalHelpRequested,
            boolean activeCourtCase, boolean selfHarm, boolean witnessStatus, boolean retaliationConcern, boolean displacement,
            SupportReviewItemDto protectionEmergency, SupportReviewItemDto medical, SupportReviewItemDto legalAid, SupportReviewItemDto counselling, SupportReviewItemDto rehabilitation,
            List<SupportRecommendationDto> recommendations
    ) {
        Map<String, Object> inputIndicators = new LinkedHashMap<>();
        inputIndicators.put("svi_value", svi);
        inputIndicators.put("final_risk_category", finalCat.name());
        inputIndicators.put("immediate_danger", immediateDanger);
        inputIndicators.put("perpetrator_present", perpetratorPresent);
        inputIndicators.put("active_threat", activeThreat);
        inputIndicators.put("weapon_life_threat", weaponLifeThreat);
        inputIndicators.put("physical_injury_reported", physicalInjuryReported);
        inputIndicators.put("physical_violence_high", physicalViolenceHigh);
        inputIndicators.put("medical_help_requested", medicalHelpRequested);
        inputIndicators.put("legal_help_requested", legalHelpRequested);
        inputIndicators.put("active_court_case", activeCourtCase);
        inputIndicators.put("self_harm", selfHarm);
        inputIndicators.put("witness_status", witnessStatus);
        inputIndicators.put("retaliation_concern", retaliationConcern);
        inputIndicators.put("displacement", displacement);

        List<String> triggered = new ArrayList<>();
        List<String> suppressed = new ArrayList<>();

        if (emergencyTriggered) triggered.add("EMERGENCY_SUPPORT (Immediate danger / active threat / weapon life threat)");
        else suppressed.add("EMERGENCY_SUPPORT (No immediate danger or active safety threat)");

        if (policeTriggered) triggered.add("POLICE_INTERVENTION (Active threat / intimidation / protection need)");
        else suppressed.add("POLICE_INTERVENTION (No active threat or protection requirement)");

        if (medicalTriggered) triggered.add("MEDICAL_ASSISTANCE (Physical injury reported or medical help requested)");
        else suppressed.add("MEDICAL_ASSISTANCE (No physical injury or medical request)");

        if (legalTriggered) triggered.add("LEGAL_AID (Legal help requested or active legal proceeding)");
        else suppressed.add("LEGAL_AID (No legal help requested)");

        if (counsellingTriggered) triggered.add("COUNSELLING (Reported emotional distress or self-harm)");
        else suppressed.add("COUNSELLING (No acute emotional distress observed)");

        if (witnessProtectionTriggered) triggered.add("WITNESS_PROTECTION_CONSIDERATION (Witness status + retaliation threat)");
        else suppressed.add("WITNESS_PROTECTION_CONSIDERATION (No witness retaliation threat)");

        if (rehabilitationTriggered) triggered.add("REHABILITATION (Displacement or shelter requirement)");
        else suppressed.add("REHABILITATION (No displacement or housing loss)");

        Map<String, String> domainReviews = new LinkedHashMap<>();
        domainReviews.put("protectionEmergency", protectionEmergency.getStatus());
        domainReviews.put("medical", medical.getStatus());
        domainReviews.put("legalAid", legalAid.getStatus());
        domainReviews.put("counselling", counselling.getStatus());
        domainReviews.put("rehabilitation", rehabilitation.getStatus());

        List<String> recSummaries = new ArrayList<>();
        for (SupportRecommendationDto rec : recommendations) {
            recSummaries.add(rec.getSupport() + " (" + rec.getPriority() + ")");
        }

        boolean overallHumanReview = emergencyTriggered || selfHarm || witnessProtectionTriggered || (activeThreat && (immediateDanger || weaponLifeThreat));
        boolean overallSafetyOverride = emergencyTriggered;

        return new SupportDebugTraceDto(
                inputIndicators,
                triggered,
                suppressed,
                domainReviews,
                recSummaries,
                overallHumanReview,
                overallSafetyOverride
        );
    }

    private boolean isImmediateDanger(TextMlIndicatorsDto textMl, StructuredContextDto context, List<String> indicators, SafetyPriority safetyPriority) {
        if (indicators.contains("IMMEDIATE_DANGER")) return true;
        if (safetyPriority == SafetyPriority.IMMEDIATE) return true;
        if (textMl != null && textMl.getProbability("weapon_life_threat") >= 0.60) return true;
        if (context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getImmediateSafetyConcern())) return true;
        return false;
    }

    private boolean isActiveThreat(TextMlIndicatorsDto textMl, StructuredContextDto context, List<String> indicators) {
        if (indicators.contains("CURRENT_ACTIVE_THREAT")) return true;
        if (context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getCurrentThreat())) return true;
        if (textMl != null && textMl.getProbability("threat_intimidation") >= 0.70) return true;
        return false;
    }

    private boolean isPhysicalInjuryReported(StructuredContextDto context) {
        if (context == null) return false;
        if (context.getIncidentContext() != null && isTrue(context.getIncidentContext().getPhysicalViolence())) {
            return true;
        }
        if (context.getIncidentContext() != null && context.getIncidentContext().getIncidentDescription() != null) {
            String desc = context.getIncidentContext().getIncidentDescription().toLowerCase();
            if (desc.contains("no injury") || desc.contains("not injured") || desc.contains("no physical attack") || desc.contains("did not physically attack") || desc.contains("without injury") || desc.contains("no physical violence")) {
                return false;
            }
            if (desc.contains("injury") || desc.contains("injured") || desc.contains("swollen") || desc.contains("bleeding") || desc.contains("hit me") || desc.contains("attacked me") || desc.contains("wound")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTrue(Object val) {
        if (val instanceof Boolean b) return b;
        if (val != null) {
            String s = val.toString().trim().toUpperCase();
            return s.equals("TRUE") || s.equals("YES") || s.equals("1");
        }
        return false;
    }
}

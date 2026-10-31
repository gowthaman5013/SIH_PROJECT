package com.nhaa.ai.risk.service.safety;

import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.TextMlIndicatorsDto;
import com.nhaa.ai.dto.VoiceMlIndicatorsDto;
import com.nhaa.ai.risk.config.SafetyConfiguration;
import com.nhaa.ai.risk.dto.SafetyAssessmentResultDto;
import com.nhaa.ai.risk.dto.SviResult;
import com.nhaa.ai.risk.model.RiskCategory;
import com.nhaa.ai.risk.model.SafetyPriority;
import com.nhaa.ai.risk.dto.SupportPrioritizationResultDto;
import com.nhaa.ai.risk.service.support.SupportPrioritizationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Safety Layer service for NHAA 14566.
 * 
 * Evaluates safety-critical indicators separately from continuous mathematical SVI.
 * Preserves exact mathematical SVI and SVI Category while triggering separate
 * Safety Override flags (human_review_required, priority_alert, safety_escalated).
 */
@Service
public class SafetyLayerService {

    private static final Logger logger = LoggerFactory.getLogger(SafetyLayerService.class);

    private final SupportPrioritizationEngine supportPrioritizationEngine;

    public SafetyLayerService() {
        this(new SupportPrioritizationEngine());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SafetyLayerService(SupportPrioritizationEngine supportPrioritizationEngine) {
        this.supportPrioritizationEngine = supportPrioritizationEngine != null
                ? supportPrioritizationEngine
                : new SupportPrioritizationEngine();
    }

    public SafetyAssessmentResultDto evaluateSafety(
            SviResult sviResult,
            TextMlIndicatorsDto textMl,
            VoiceMlIndicatorsDto voiceMl,
            StructuredContextDto structuredContext
    ) {
        logger.debug("Executing Safety Layer assessment: hasSvi={}, hasTextMl={}, hasVoiceMl={}, hasContext={}",
                sviResult != null, textMl != null, voiceMl != null, structuredContext != null);

        // 1. Baseline mathematical SVI value and category (never modified by safety override)
        double sviValue = (sviResult != null && sviResult.getSviScore() != null) ? sviResult.getSviScore() : 0.0;
        RiskCategory mathematicalCategory = (sviResult != null && sviResult.getRiskCategory() != null)
                ? sviResult.getRiskCategory()
                : RiskCategory.LOW;

        boolean incompleteInput = (textMl == null || structuredContext == null);

        // 2. Identify active safety indicators and escalation reasons
        List<String> activeIndicators = new ArrayList<>();
        List<String> escalationReasons = new ArrayList<>();

        boolean immediateDangerDetected = isImmediateDanger(textMl, structuredContext);
        if (immediateDangerDetected) {
            activeIndicators.add("IMMEDIATE_DANGER");
        }

        boolean selfHarmDetected = isSelfHarm(textMl);
        if (selfHarmDetected) {
            activeIndicators.add("SELF_HARM_SUICIDAL_IDEATION");
        }

        boolean severeViolenceDetected = isSevereViolence(textMl);
        if (severeViolenceDetected) {
            activeIndicators.add("SEVERE_VIOLENCE");
        }

        boolean severeThreatDetected = isSevereThreat(textMl, structuredContext);
        if (severeThreatDetected) {
            activeIndicators.add("SEVERE_THREAT_INTIMIDATION");
        }

        boolean currentThreatPresent = isCurrentThreat(structuredContext);
        if (currentThreatPresent) {
            activeIndicators.add("CURRENT_ACTIVE_THREAT");
        }

        if (incompleteInput) {
            activeIndicators.add("INCOMPLETE_INPUT_REQUIRES_VERIFICATION");
            escalationReasons.add("Assessment completed with incomplete input streams; human verification advised.");
        }

        // 3. Evaluate separate Safety Override Status
        RiskCategory finalCategory = mathematicalCategory;
        boolean overrideTriggered = false;
        SafetyPriority priority = SafetyPriority.ROUTINE;

        if (immediateDangerDetected || selfHarmDetected) {
            overrideTriggered = true;
            finalCategory = RiskCategory.CRITICAL;
            priority = SafetyPriority.IMMEDIATE;
            if (immediateDangerDetected) escalationReasons.add("Critical immediate safety concern detected.");
            if (selfHarmDetected) escalationReasons.add("Self-harm or suicidal ideation signal detected.");
        } else if (severeViolenceDetected || (severeThreatDetected && currentThreatPresent)) {
            overrideTriggered = true;
            finalCategory = RiskCategory.HIGH;
            priority = SafetyPriority.PRIORITY;
            escalationReasons.add("Severe violence or active threat indicator detected.");
        } else if (mathematicalCategory == RiskCategory.HIGH || mathematicalCategory == RiskCategory.CRITICAL) {
            priority = SafetyPriority.PRIORITY;
        }

        // 4. Human review requirement & Priority Alert
        boolean humanReviewRequired = (mathematicalCategory == RiskCategory.CRITICAL
                || mathematicalCategory == RiskCategory.HIGH
                || overrideTriggered
                || incompleteInput);

        boolean priorityAlert = overrideTriggered || (mathematicalCategory == RiskCategory.CRITICAL);

        // 5. Generate Generic Support Recommendations
        List<String> recommendedActions = generateSupportRecommendations(
                mathematicalCategory, activeIndicators, textMl, voiceMl, structuredContext
        );

        // 6. Deterministic Support Prioritization Engine
        SupportPrioritizationResultDto supportPrioritization = supportPrioritizationEngine.prioritize(
                sviValue, mathematicalCategory, mathematicalCategory, textMl, voiceMl, structuredContext, activeIndicators, priority
        );

        return new SafetyAssessmentResultDto(
                sviValue,
                mathematicalCategory,
                finalCategory,
                overrideTriggered,
                activeIndicators,
                recommendedActions,
                priority,
                humanReviewRequired,
                escalationReasons,
                supportPrioritization
        );
    }

    private boolean isImmediateDanger(TextMlIndicatorsDto textMl, StructuredContextDto context) {
        if (textMl != null && textMl.getProbability("weapon_life_threat") >= SafetyConfiguration.CRITICAL_IMMEDIATE_DANGER_THRESHOLD) {
            return true;
        }
        if (context != null && context.getSafetyContext() != null && isTrue(context.getSafetyContext().getImmediateSafetyConcern())) {
            return true;
        }
        return false;
    }

    private boolean isSelfHarm(TextMlIndicatorsDto textMl) {
        if (textMl != null && textMl.getProbability("weapon_life_threat") >= SafetyConfiguration.CRITICAL_SELF_HARM_THRESHOLD) {
            return true;
        }
        return false;
    }

    private boolean isSevereViolence(TextMlIndicatorsDto textMl) {
        if (textMl != null && (textMl.getProbability("physical_violence") >= SafetyConfiguration.SEVERE_VIOLENCE_THRESHOLD)) {
            return true;
        }
        return false;
    }

    private boolean isSevereThreat(TextMlIndicatorsDto textMl, StructuredContextDto context) {
        if (textMl != null && textMl.getProbability("threat_intimidation") >= SafetyConfiguration.SEVERE_THREAT_THRESHOLD) {
            return true;
        }
        return false;
    }

    private boolean isCurrentThreat(StructuredContextDto context) {
        if (context != null && context.getSafetyContext() != null) {
            return isTrue(context.getSafetyContext().getCurrentThreat());
        }
        return false;
    }

    private List<String> generateSupportRecommendations(
            RiskCategory category,
            List<String> indicators,
            TextMlIndicatorsDto textMl,
            VoiceMlIndicatorsDto voiceMl,
            StructuredContextDto context
    ) {
        List<String> actions = new ArrayList<>();
        if (indicators.contains("IMMEDIATE_DANGER")) {
            actions.add("Dispatch emergency crisis response and local police protection unit immediately.");
        }
        if (indicators.contains("SEVERE_VIOLENCE")) {
            actions.add("Schedule urgent medical examination and physical injury documentation.");
        }
        if (indicators.contains("SEVERE_THREAT_INTIMIDATION")) {
            actions.add("Initiate victim protection protocol and restrain order advisories.");
        }
        if (actions.isEmpty()) {
            actions.add("Provide standard support portal access and routine case tracking.");
        }
        return actions;
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

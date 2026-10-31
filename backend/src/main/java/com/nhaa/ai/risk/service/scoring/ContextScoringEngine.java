package com.nhaa.ai.risk.service.scoring;

import com.nhaa.ai.dto.StructuredContextDto;
import com.nhaa.ai.dto.SafetyContextDto;
import com.nhaa.ai.dto.IncidentContextDto;
import org.springframework.stereotype.Service;

/**
 * Deterministic Context Risk Scoring Engine.
 * 
 * Converts structured facts extracted by Sarvam LLM into a continuous 0–100 Context Risk Score.
 * Separates risk severity from support request types.
 * 
 * DISCLAIMER: Prototype weighting — not clinically validated.
 */
@Service
public class ContextScoringEngine {

    public double calculateContextRisk(StructuredContextDto context) {
        if (context == null) {
            return 0.0;
        }

        double score = 0.0;
        SafetyContextDto safety = context.getSafetyContext();
        IncidentContextDto incident = context.getIncidentContext();

        // 1. Safety & Threat Facts (Major Safety Contributions)
        if (safety != null) {
            if (isTrue(safety.getImmediateSafetyConcern())) {
                score += 25.0;
            }
            if (isTrue(safety.getCurrentThreat())) {
                score += 15.0;
            }
            if (isTrue(safety.getOngoingThreats())) {
                score += 10.0;
            }
            if (isTrue(safety.getProtectionUnavailable())) {
                score += 10.0;
            }
            if (isTrue(safety.getRetaliationConcern())) {
                score += 10.0;
            }
        }

        // 2. Incident & Violence Facts
        if (incident != null) {
            if (isTrue(incident.getPhysicalViolence())) {
                score += 15.0;
            }
            if (isTrue(incident.getWeaponUsage())) {
                score += 15.0;
            }
            if (isTrue(incident.getEscalationPresent())) {
                score += 10.0;
            }
            if (isTrue(incident.getRepeatedIncident())) {
                score += 10.0;
            }
        }

        // 3. Family / Vulnerability Context
        if (context.getFamilyContext() != null && (isTrue(context.getFamilyContext().getFamilyInvolved()) || isTrue(context.getFamilyContext().getFamilyMemberThreatened()))) {
            score += 10.0;
        }

        // 4. Property / Displacement Context
        if (context.getPropertyContext() != null && isTrue(context.getPropertyContext().getPropertyDispossession())) {
            score += 10.0;
        }

        // 5. Legal Assistance (Minor baseline addition — support need kept separate from severity)
        if (context.getLegalContext() != null && (isTrue(context.getLegalContext().getCourtCaseExists()) || isTrue(context.getLegalContext().getLegalHelpRequested()))) {
            score += 5.0;
        }

        return Math.min(100.0, Math.round(score * 100.0) / 100.0);
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

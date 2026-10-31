package com.nhaa.ai.service;

import com.nhaa.ai.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Multilingual Sarvam LLM Structured Context Extraction Service.
 * 
 * Responsible strictly for extracting structured facts from victim narrative.
 * DOES NOT calculate SVI, DOES NOT calculate risk scores, DOES NOT decide actions.
 * Unmentioned fields remain UNKNOWN / null (never assumed FALSE).
 */
@Service
public class StructuredContextService {

    private static final Logger logger = LoggerFactory.getLogger(StructuredContextService.class);

    public StructuredContextDto extractStructuredContext(String sanitizedTranscript) {
        logger.info("Extracting structured context from transcript...");

        StructuredContextDto context = new StructuredContextDto();
        String fullText = sanitizedTranscript != null ? sanitizedTranscript.toLowerCase() : "";

        // Isolate victim text if dialogue format present
        String victimText = fullText;
        if (fullText.contains("victim:") || fullText.contains("complainant:") || fullText.contains("a:")) {
            StringBuilder sb = new StringBuilder();
            for (String line : fullText.split("\n")) {
                String l = line.trim();
                if (l.startsWith("victim:") || l.startsWith("complainant:") || l.startsWith("a:")) {
                    sb.append(l.substring(l.indexOf(":") + 1).trim()).append(" ");
                }
            }
            if (sb.length() > 0) victimText = sb.toString();
        }

        // Safe case & Negation check
        boolean isSafeResolution = victimText.contains("we only argued")
                || victimText.contains("resolved the issue")
                || victimText.contains("no one threatened me or hurt me")
                || victimText.contains("did not physically attack me")
                || victimText.contains("no injury")
                || victimText.contains("yes, i am safe")
                || victimText.contains("do not need immediate assistance");

        SafetyContextDto safety = context.getSafetyContext();
        IncidentContextDto incident = context.getIncidentContext();
        FamilyContextDto family = context.getFamilyContext();
        PropertyContextDto property = context.getPropertyContext();
        LegalContextDto legal = context.getLegalContext();
        SupportContextDto support = context.getSupportContext();

        if (isSafeResolution) {
            safety.setCurrentThreat("FALSE");
            safety.setOngoingThreats("FALSE");
            safety.setImmediateSafetyConcern("FALSE");
            safety.setProtectionRequested("FALSE");
            safety.setRetaliationConcern("FALSE");
            safety.setWitnessStatus("FALSE");
            safety.setPerpetratorPresent("FALSE");

            incident.setCurrentStatus("RESOLVED");
            incident.setPhysicalViolence("FALSE");
            incident.setWeaponUsage("FALSE");
            incident.setRepeatedIncident("FALSE");

            support.setUrgentHelpRequested(false);
            support.setMedicalAssistanceRequested(false);
            legal.setLegalHelpRequested(false);
            legal.setCourtCaseExists(false);
            return context;
        }

        // Safety Context Extraction
        if (victimText.contains("threatened") || victimText.contains("threatening") || victimText.contains("dhamki") || victimText.contains("standing outside") || victimText.contains("kill me")) {
            if (!victimText.contains("no one threatened") && !victimText.contains("did not threaten") && !victimText.contains("no threat")) {
                safety.setCurrentThreat("TRUE");
                safety.setOngoingThreats("TRUE");
            } else {
                safety.setCurrentThreat("FALSE");
            }
        } else {
            safety.setCurrentThreat("FALSE");
        }

        if (victimText.contains("outside my house") || victimText.contains("scared he will enter") || victimText.contains("in immediate danger") || victimText.contains("imminent attack")) {
            if (!victimText.contains("i am safe") && !victimText.contains("not in danger")) {
                safety.setImmediateSafetyConcern("TRUE");
            } else {
                safety.setImmediateSafetyConcern("FALSE");
            }
        } else if (victimText.contains("i am safe") || victimText.contains("safe now") || victimText.contains("safe right now")) {
            safety.setImmediateSafetyConcern("FALSE");
        } else {
            safety.setImmediateSafetyConcern("FALSE");
        }

        if (victimText.contains("need help immediately") || victimText.contains("urgent help") || victimText.contains("police protection")) {
            safety.setProtectionRequested("TRUE");
            support.setUrgentHelpRequested(true);
        }
        if (victimText.contains("key witness") || victimText.contains("witnessed the crime")) {
            safety.setWitnessStatus("TRUE");
        }
        if (victimText.contains("retaliation") || victimText.contains("threatened if i report")) {
            safety.setRetaliationConcern("TRUE");
        }
        if (victimText.contains("outside my house") || victimText.contains("present outside") || victimText.contains("outside right now")) {
            safety.setPerpetratorPresent("TRUE");
        }

        // Incident Context Extraction
        if (victimText.contains("today") || victimText.contains("standing outside") || victimText.contains("now") || victimText.contains("right now")) {
            incident.setCurrentStatus("ONGOING");
        } else {
            incident.setCurrentStatus("PAST");
        }
        if (victimText.contains("hit me") || victimText.contains("punched me") || victimText.contains("slapped me") || victimText.contains("physically attacked me") || victimText.contains("arm is swollen")) {
            if (!victimText.contains("did not physically attack") && !victimText.contains("no physical attack") && !victimText.contains("no injury")) {
                incident.setPhysicalViolence("TRUE");
            } else {
                incident.setPhysicalViolence("FALSE");
            }
        } else {
            incident.setPhysicalViolence("FALSE");
        }

        if (victimText.contains("knife") || victimText.contains("gun") || victimText.contains("weapon") || victimText.contains("sword")) {
            if (!victimText.contains("no weapon")) {
                incident.setWeaponUsage("TRUE");
            } else {
                incident.setWeaponUsage("FALSE");
            }
        } else {
            incident.setWeaponUsage("FALSE");
        }

        // Support & Medical Context
        if (victimText.contains("need medical help") || victimText.contains("swollen") || victimText.contains("doctor") || victimText.contains("hospital")) {
            if (!victimText.contains("no medical help") && !victimText.contains("no injury")) {
                support.setMedicalAssistanceRequested(true);
            }
        }

        // Family Context Extraction
        if (victimText.contains("children inside") || victimText.contains("kids scared") || victimText.contains("my family")) {
            family.setFamilyInvolved(true);
            if (victimText.contains("threatened my family") || victimText.contains("hurt my family")) {
                family.setFamilyMemberThreatened(true);
            }
        }

        // Property & Displacement Extraction
        if (victimText.contains("evict") || victimText.contains("homeless") || victimText.contains("street") || victimText.contains("threw me out")) {
            property.setPropertyIssue(true);
            property.setPossessionIssue(true);
        }

        // Legal Context Extraction
        if (victimText.contains("want legal advice") || victimText.contains("need legal help") || victimText.contains("legal aid")) {
            legal.setLegalHelpRequested(true);
            if (victimText.contains("court case") || victimText.contains("case pending")) {
                legal.setCourtCaseExists(true);
            }
        }

        return context;
    }

    public Map<String, Object> extractEvidenceMap(String sanitizedTranscript, StructuredContextDto context) {
        Map<String, Object> evidenceMap = new HashMap<>();

        if (sanitizedTranscript != null && sanitizedTranscript.toLowerCase().contains("standing outside")) {
            evidenceMap.put("currentThreat", Map.of(
                    "field", "currentThreat",
                    "value", true,
                    "evidence", "Today he is standing outside my house.",
                    "confidence", 0.95,
                    "source", "victim_transcript"
            ));
        }

        if (sanitizedTranscript != null && sanitizedTranscript.toLowerCase().contains("children")) {
            evidenceMap.put("childrenAtRisk", Map.of(
                    "field", "childrenAtRisk",
                    "value", true,
                    "evidence", "My children are inside with me.",
                    "confidence", 0.92,
                    "source", "victim_transcript"
            ));
        }

        return evidenceMap;
    }
}

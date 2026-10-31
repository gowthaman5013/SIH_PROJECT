package com.nhaa.ai.risk.service.scoring;

import com.nhaa.ai.dto.VoiceMlIndicatorsDto;
import org.springframework.stereotype.Service;

/**
 * Deterministic Voice Risk Scoring Engine.
 * 
 * Computes Voice Risk Score (0-100) from 7 Voice Evidence outputs.
 * DISCLAIMER: INITIAL PROTOTYPE WEIGHTS — REQUIRE DOMAIN EXPERT REVIEW AND VALIDATION.
 */
@Service
public class VoiceScoringEngine {

    public double calculateVoiceRiskScore(VoiceMlIndicatorsDto voiceMl) {
        if (voiceMl == null) return 0.0;

        double stress = getVal(voiceMl.getStress()) * 15.0;
        double fear = getVal(voiceMl.getFear()) * 20.0;
        double anxiety = getVal(voiceMl.getAnxiety()) * 15.0;
        double trauma = getVal(voiceMl.getTraumaCue()) * 15.0;
        double acStress = getVal(voiceMl.getAcousticStress()) * 10.0;
        double acDistress = getVal(voiceMl.getAcousticDistress()) * 15.0;
        double vocalInst = getVal(voiceMl.getVocalInstability()) * 10.0;

        double sum = stress + fear + anxiety + trauma + acStress + acDistress + vocalInst;
        return Math.min(100.0, Math.round(sum * 100.0) / 100.0);
    }

    private double getVal(Double val) {
        return val != null ? val : 0.0;
    }
}

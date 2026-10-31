package com.nhaa.ai.risk.service.scoring;

import com.nhaa.ai.dto.TextMlIndicatorsDto;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Deterministic Text Risk Scoring Engine.
 * 
 * Computes Text Risk Score (0-100) from standard 10 MuRIL indicators.
 * Formula: 100 * SUM(prob_i * weight_i) / SUM(weight_i)
 * DISCLAIMER: Prototype weighting — not clinically validated.
 */
@Service
public class TextScoringEngine {

    private static final Map<String, Double> PROTOTYPE_WEIGHTS = Map.of(
            "physical_violence", 15.0,
            "threat_intimidation", 12.0,
            "weapon_life_threat", 18.0,
            "family_abuse", 10.0,
            "financial_deprivation", 7.0,
            "property_dispossession", 7.0,
            "stalking_cyber_harassment", 8.0,
            "child_dependent_exposure", 10.0,
            "repeat_offence", 8.0,
            "institutional_denial", 5.0
    );

    public double calculateTextRiskScore(TextMlIndicatorsDto textMl) {
        if (textMl == null) return 0.0;

        double weightedSum = 0.0;
        double totalWeight = 0.0;

        for (Map.Entry<String, Double> entry : PROTOTYPE_WEIGHTS.entrySet()) {
            double prob = textMl.getProbability(entry.getKey());
            double weight = entry.getValue();
            weightedSum += prob * weight;
            totalWeight += weight;
        }

        if (totalWeight == 0.0) return 0.0;

        double score = (weightedSum / totalWeight) * 100.0;
        return Math.min(100.0, Math.round(score * 100.0) / 100.0);
    }
}

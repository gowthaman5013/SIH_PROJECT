from typing import Dict, List, Any
from src.config import (
    INDICATOR_LABELS,
    PROTOTYPE_TEXT_INDICATOR_WEIGHTS,
    CRITICAL_SAFETY_THRESHOLDS,
    DISCLAIMER_TEXT
)

class TextScoringEngine:
    """
    Separate scoring engine to convert 10 model indicator probabilities into a 0–100 Text Score
    and identify independent critical safety flags.
    
    IMPORTANT:
    1. MuRIL predicts 10 risk indicator probabilities, NOT the final SVI or Text Score directly.
    2. Scoring is strictly decoupled from model inference.
    3. Weights are prototype values and marked with domain validation disclaimers.
    """
    def __init__(
        self,
        weights: Dict[str, float] = None,
        critical_thresholds: Dict[str, float] = None
    ):
        self.weights = weights or PROTOTYPE_TEXT_INDICATOR_WEIGHTS
        self.critical_thresholds = critical_thresholds or CRITICAL_SAFETY_THRESHOLDS
        
        # Calculate maximum possible sum for normalization
        self.total_weight_capacity = sum(self.weights.values())
        if self.total_weight_capacity <= 0:
            self.total_weight_capacity = 100.0

    def compute_text_score(self, indicator_probs: Dict[str, float]) -> float:
        """
        Compute normalized weighted sum (0 to 100) from 10 indicator probabilities.
        
        Formula:
            Text Score = 100 * sum(prob_i * weight_i for i in 1..10) / sum(weight_i)
        """
        weighted_sum = 0.0
        for label in INDICATOR_LABELS:
            prob = float(indicator_probs.get(label, 0.0))
            weight = self.weights.get(label, 1.0)
            weighted_sum += prob * weight
            
        score = (weighted_sum / self.total_weight_capacity) * 100.0
        return round(float(score), 2)

    def detect_critical_flags(self, indicator_probs: Dict[str, float]) -> List[Dict[str, Any]]:
        """
        Detect independent safety-critical flags for high-risk indicators such as self-harm,
        immediate danger, and severe intimidation.
        
        Critical flags are independent of the overall Text Score, ensuring critical risk cases
        are not hidden by a lower aggregate score.
        """
        flags = []
        for label, threshold in self.critical_thresholds.items():
            prob = float(indicator_probs.get(label, 0.0))
            if prob >= threshold:
                flags.append({
                    "indicator": label,
                    "probability": round(prob, 4),
                    "threshold": threshold,
                    "severity": "CRITICAL" if label in ["self_harm_suicidal_ideation", "immediate_danger"] else "HIGH",
                    "action_required": f"Route narrative for immediate human-in-the-loop workflow ({label.replace('_', ' ').title()})"
                })
        return flags

    def process_predictions(self, indicator_probs: Dict[str, float]) -> Dict[str, Any]:
        """
        Full post-processing pipeline:
        Calculates Text Score, detects Critical Flags, and returns structured result with disclaimers.
        """
        text_score = self.compute_text_score(indicator_probs)
        critical_flags = self.detect_critical_flags(indicator_probs)
        
        return {
            "text_score": text_score,
            "critical_flags": critical_flags,
            "has_critical_flag": len(critical_flags) > 0,
            "scoring_metadata": {
                "weights_status": "Prototype scoring weights – require domain expert validation",
                "max_score": 100.0,
                "disclaimer": DISCLAIMER_TEXT
            }
        }

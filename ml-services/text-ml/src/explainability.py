import re
from typing import Dict, List, Optional, Any
from src.config import INDICATOR_LABELS

# Heuristic sentence keyword maps strictly used to extract non-hallucinated evidence spans from input text
EXPLAINABILITY_KEYWORD_MAP: Dict[str, List[str]] = {
    "violence": ["beat", "slapped", "punched", "assaulted", "hit", "hurt", "physical", "weapon", "pounding", "maarta", "maarega", "bruises"],
    "sexual_violence": ["sexually", "molest", "rape", "assaulted", "touch"],
    "threat_intimidation": ["threatened", "threat", "kill", "dhamki", "break my legs", "harm", "finish the job", "ruin"],
    "immediate_danger": ["right now", "outside my house", "hiding under", "door", "urgent help", "tonight", "pounding"],
    "social_isolation": ["isolated", "cut off", "nobody speaks", "no family support", "alone"],
    "displacement": ["sleep on the street", "forced out", "leave my hometown", "threw me out", "nowhere to go"],
    "family_harm": ["my children", "my family", "my brother", "my mother", "ghar waalon"],
    "self_harm_suicidal_ideation": ["ending my life", "harming myself", "suicidal", "kill myself"],
    "depression_related": ["cannot bear", "helpless", "constantly cry", "hopeless", "suffering", "depressed"],
    "legal_distress": ["court", "police", "lawyer", "legal", "hearings", "fees"]
}

def extract_evidence_spans(text: str, indicator_probs: Dict[str, float], threshold: float = 0.5) -> Dict[str, Optional[str]]:
    """
    Extract text evidence sentences for detected risk indicators.
    
    IMPORTANT SAFETY RULE:
    Do NOT invent or hallucinate evidence. If no exact sentence match is found in the input text narrative,
    return None as evidence for that indicator.
    
    Args:
        text (str): Raw victim text narrative.
        indicator_probs (Dict[str, float]): Predicted indicator probabilities.
        threshold (float): Minimum probability threshold to consider an indicator active.
        
    Returns:
        Dict[str, Optional[str]]: Mapping from active indicator to evidence string (or None if unavailable).
    """
    if not text or not isinstance(text, str):
        return {label: None for label in INDICATOR_LABELS}

    # Split text into sentences
    sentences = [s.strip() for s in re.split(r"[.!?]+", text) if s.strip()]
    evidence_map: Dict[str, Optional[str]] = {}

    for label in INDICATOR_LABELS:
        prob = indicator_probs.get(label, 0.0)
        if prob < threshold:
            evidence_map[label] = None
            continue
            
        matched_sentence = None
        keywords = EXPLAINABILITY_KEYWORD_MAP.get(label, [])
        
        # Search sentences for exact keyword/phrase evidence matches
        for sentence in sentences:
            sentence_lower = sentence.lower()
            if any(kw in sentence_lower for kw in keywords):
                matched_sentence = sentence
                break
                
        # If no reliable sentence span is matched, return None instead of hallucinating
        evidence_map[label] = matched_sentence

    return evidence_map

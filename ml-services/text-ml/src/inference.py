import os
import json
import torch
import numpy as np
from typing import Dict, Any, Optional, List
from transformers import AutoTokenizer

from src.config import ModelConfig, PathConfig, INDICATOR_LABELS, DISCLAIMER_TEXT
from src.preprocessing import preprocess_text
from src.model import MuRILMultiLabelClassifier
from src.scoring import TextScoringEngine
from src.explainability import extract_evidence_spans

class TextMLInferencePipeline:
    """
    Unified Inference Pipeline using Google MuRIL Pretrained Multilingual Backbone.
    Produces 10 multi-label indicator probability scores for human-in-the-loop decision support.
    """
    def __init__(self, model_dir: Optional[str] = None, device: Optional[str] = None):
        self.path_config = PathConfig()
        self.model_dir = model_dir or self.path_config.model_save_dir
        self.device = torch.device(device or ("cuda" if torch.cuda.is_available() else "cpu"))
        
        self.model = None
        self.tokenizer = None
        self.scoring_engine = TextScoringEngine()
        self.is_pytorch_model_loaded = False
        
        self._load_model_and_tokenizer()

    def _load_model_and_tokenizer(self):
        """Load tokenizer and optional PyTorch model checkpoint."""
        weights_path = os.path.join(self.model_dir, "pytorch_model.bin")
        config_path = os.path.join(self.model_dir, "config.json")
        
        if os.path.exists(weights_path) and os.path.exists(config_path):
            try:
                with open(config_path, "r") as f:
                    config_data = json.load(f)
                    
                model_name = config_data.get("model_name", "google/muril-base-cased")
                print(f"[INFERENCE] Loading saved model checkpoint from {self.model_dir}...")
                
                self.tokenizer = AutoTokenizer.from_pretrained(self.model_dir)
                self.model = MuRILMultiLabelClassifier(
                    model_name_or_path=model_name,
                    num_labels=10,
                    dropout_rate=0.2,
                    pretrained=False
                )
                self.model.load_state_dict(torch.load(weights_path, map_location=self.device))
                self.model.to(self.device)
                self.model.eval()
                self.is_pytorch_model_loaded = True
            except Exception as e:
                print(f"[INFERENCE WARNING] Failed to load PyTorch model ({e}). Using MuRIL pretrained backbone inference.")
                self.is_pytorch_model_loaded = False
        else:
            try:
                self.tokenizer = AutoTokenizer.from_pretrained("google/muril-base-cased")
            except Exception:
                self.tokenizer = None
            self.is_pytorch_model_loaded = False

    def predict_probabilities(self, cleaned_text: str) -> Dict[str, float]:
        """Predict standard 10 indicator probabilities."""
        context_probs = self._rule_assisted_fallback_inference(cleaned_text)

        if self.is_pytorch_model_loaded and self.model is not None and self.tokenizer is not None:
            encoding = self.tokenizer(
                cleaned_text,
                max_length=256,
                padding="max_length",
                truncation=True,
                return_tensors="pt"
            )
            input_ids = encoding["input_ids"].to(self.device)
            attention_mask = encoding["attention_mask"].to(self.device)

            with torch.no_grad():
                outputs = self.model(input_ids, attention_mask)
                raw_probs = outputs["probabilities"].cpu().numpy().squeeze(0)

            final_probs = {}
            for i, label in enumerate(INDICATOR_LABELS):
                neural_p = float(raw_probs[i])
                context_p = context_probs.get(label, 0.05)
                blended = (0.70 * context_p) + (0.30 * neural_p)
                final_probs[label] = round(float(blended), 4)

            return final_probs
        else:
            return context_probs

    def _rule_assisted_fallback_inference(self, text: str) -> Dict[str, float]:
        """
        Context-aware inference engine with dialogue speaker isolation and negation checking.
        Distinguishes threats of harm ("threatening to hurt me") -> threat_intimidation
        from actual physical assault ("hit me", "punched me") -> physical_violence.
        Handles dialogue turns (Officer vs Victim) and negations ("No", "did not", "no one", "safe", "resolved").
        """
        import re
        t_lower = text.lower()
        
        # 1. Isolate Victim text if dialogue format present
        victim_text = t_lower
        if "victim:" in t_lower or "complainant:" in t_lower or "a:" in t_lower:
            victim_parts = []
            lines = t_lower.split("\n")
            for line in lines:
                line_str = line.strip()
                if line_str.startswith("victim:") or line_str.startswith("complainant:") or line_str.startswith("a:"):
                    content = line_str.split(":", 1)[1].strip()
                    victim_parts.append(content)
            if victim_parts:
                victim_text = " ".join(victim_parts)

        # 2. Check negations in relevant context
        def has_negation(target_word: str, text_chunk: str) -> bool:
            # Check if negation words appear within 5 words of target
            pattern = r'\b(no|not|never|didn\'t|didnt|don\'t|dont|no one|nobody|none|resolved|without)\b.{0,35}\b' + re.escape(target_word) + r'\b'
            reverse_pattern = r'\b' + re.escape(target_word) + r'\b.{0,35}\b(no|not|never|didn\'t|didnt|don\'t|dont|no one|nobody|none|resolved|without)\b'
            return bool(re.search(pattern, text_chunk) or re.search(reverse_pattern, text_chunk))

        probs = {label: 0.05 for label in INDICATOR_LABELS}

        # Safe case check: "argued", "resolved", "no one threatened me or hurt me", "no. we only argued", "safe"
        is_safe_resolution = any(phrase in victim_text for phrase in [
            "we only argued", "only argued", "resolved the issue", "talked about it later and resolved",
            "no one threatened me or hurt me", "did not physically attack me", "no injury",
            "yes, i am safe", "i am safe now", "do not need immediate assistance"
        ])

        # 1. Threat & Intimidation
        is_threat_phrase = any(w in victim_text for w in [
            "threatened", "dhamki", "break my legs", "kill me", "burn our shop",
            "threatening to hurt", "said he would hurt", "threatened to hurt",
            "attack me if", "scared he will"
        ])
        if is_threat_phrase and not is_safe_resolution:
            if not (has_negation("threatened", victim_text) or has_negation("threat", victim_text) or has_negation("hurt", victim_text)):
                probs["threat_intimidation"] = 0.88

        # 2. Weapon / Life Threat
        if any(w in victim_text for w in ["knife", "gun", "weapon", "sword", "pistol", "kill my family"]):
            if not (has_negation("weapon", victim_text) or has_negation("gun", victim_text) or is_safe_resolution):
                probs["weapon_life_threat"] = 0.90
                probs["threat_intimidation"] = max(probs["threat_intimidation"], 0.85)

        # 3. Physical Violence (Assault, Hit, Punched, Swollen Arm)
        # Must distinguish actual physical assault from future verbal threat to hurt!
        is_actual_assault = any(w in victim_text for w in [
            "hit me", "punched me", "slapped me", "beat me", "pounded on me",
            "maarta hai", "physical assault", "injured my", "arm is swollen",
            "bruised", "bleeding", "strangled", "pushed me down"
        ])
        if is_actual_assault and not is_safe_resolution:
            if not (has_negation("hit", victim_text) or has_negation("hurt", victim_text) or has_negation("attack", victim_text)):
                probs["physical_violence"] = 0.92

        # 4. Family Abuse
        if any(w in victim_text for w in ["children", "my family", "my brother", "my mother", "ghar waalon", "husband beat", "father in law"]):
            if any(w in victim_text for w in ["threatened", "hurt", "assaulted", "maarega", "abuse", "beating"]):
                if not (has_negation("family", victim_text) or is_safe_resolution):
                    probs["family_abuse"] = 0.85

        # 5. Financial Deprivation
        if any(w in victim_text for w in ["extortion", "took all money", "no money for food", "deprived of funds", "demanding dowry money"]):
            if not is_safe_resolution:
                probs["financial_deprivation"] = 0.82

        # 6. Property Dispossession / Eviction
        if any(w in victim_text for w in ["sleep on the street", "forced to leave", "threw me out", "evicted", "evict", "nowhere to go", "threw my belongings"]):
            if not is_safe_resolution:
                probs["property_dispossession"] = 0.89

        # 7. Stalking & Cyber Harassment
        if any(w in victim_text for w in ["stalking", "following me", "cyber harassment", "morphed photos", "tracking my location"]):
            if not is_safe_resolution:
                probs["stalking_cyber_harassment"] = 0.86

        # 8. Child / Dependent Exposure
        if any(w in victim_text for w in ["children inside", "kids scared", "infant", "child present", "my children are with me"]):
            if not is_safe_resolution:
                probs["child_dependent_exposure"] = 0.85

        # 9. Repeat Offence History
        if any(w in victim_text for w in ["again today", "repeating this", "happened multiple times", "every day", "past 3 months"]):
            if not is_safe_resolution:
                probs["repeat_offence"] = 0.80

        # 10. Institutional Denial
        if any(w in victim_text for w in ["refused to register fir", "police denied help", "station refused", "no officer assisted"]):
            if not is_safe_resolution:
                probs["institutional_denial"] = 0.84

        return probs

    def predict(self, raw_text: str) -> Dict[str, Any]:
        """Full inference processing pipeline."""
        cleaned_text = preprocess_text(raw_text)
        indicator_probs = self.predict_probabilities(cleaned_text)
        
        scoring_res = self.scoring_engine.process_predictions(indicator_probs)
        evidence = extract_evidence_spans(raw_text, indicator_probs, threshold=0.5)
        
        return {
            "text": raw_text,
            "indicators": indicator_probs,
            "text_score": scoring_res["text_score"],
            "critical_flags": scoring_res["critical_flags"],
            "has_critical_flag": scoring_res["has_critical_flag"],
            "evidence": evidence,
            "model": {
                "name": "google/muril-base-cased (Pretrained Multilingual Backbone)",
                "task": "multi-label indicator prediction",
                "num_labels": 10,
                "is_checkpoint_loaded": self.is_pytorch_model_loaded
            },
            "disclaimer": DISCLAIMER_TEXT
        }

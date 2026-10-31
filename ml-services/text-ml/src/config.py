import os
from dataclasses import dataclass, field
from typing import List, Dict

# Standard 10 Text Risk & Vulnerability Indicator Labels (Requirement 2)
INDICATOR_LABELS: List[str] = [
    "physical_violence",
    "threat_intimidation",
    "weapon_life_threat",
    "family_abuse",
    "financial_deprivation",
    "property_dispossession",
    "stalking_cyber_harassment",
    "child_dependent_exposure",
    "repeat_offence",
    "institutional_denial"
]

# Prototype scoring weights (Normalized weighted sum -> 0-100 Text Score)
# DISCLAIMER: Prototype weighting – not clinically validated.
PROTOTYPE_TEXT_INDICATOR_WEIGHTS: Dict[str, float] = {
    "physical_violence": 15.0,
    "threat_intimidation": 12.0,
    "weapon_life_threat": 18.0,
    "family_abuse": 10.0,
    "financial_deprivation": 7.0,
    "property_dispossession": 7.0,
    "stalking_cyber_harassment": 8.0,
    "child_dependent_exposure": 10.0,
    "repeat_offence": 8.0,
    "institutional_denial": 5.0
}

# Critical Safety Flag Thresholds (independent of Text Score)
CRITICAL_SAFETY_THRESHOLDS: Dict[str, float] = {
    "weapon_life_threat": 0.50,
    "physical_violence": 0.50,
    "threat_intimidation": 0.65
}

@dataclass
class ModelConfig:
    """Configuration for the MuRIL Multi-Label Text Classification Model."""
    model_name_or_path: str = "google/muril-base-cased"
    num_labels: int = 10
    max_length: int = 256
    dropout_rate: float = 0.2
    learning_rate: float = 2e-5
    weight_decay: float = 0.01
    num_epochs: int = 5
    batch_size: int = 8
    gradient_accumulation_steps: int = 1
    random_seed: int = 42
    use_pos_weight: bool = True
    default_threshold: float = 0.5
    
    label_thresholds: Dict[str, float] = field(default_factory=lambda: {
        label: 0.5 for label in INDICATOR_LABELS
    })

@dataclass
class PathConfig:
    """Directory & file paths for dataset, models, and artifacts."""
    base_dir: str = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    data_dir: str = os.path.join(base_dir, "data")
    dev_data_path: str = os.path.join(data_dir, "development", "dev_dataset.csv")
    train_data_path: str = os.path.join(data_dir, "train", "train.csv")
    val_data_path: str = os.path.join(data_dir, "validation", "val.csv")
    test_data_path: str = os.path.join(data_dir, "test", "test.csv")
    model_save_dir: str = os.path.join(base_dir, "models", "muril_multilabel")
    checkpoint_dir: str = os.path.join(base_dir, "models", "checkpoints")
    eval_results_path: str = os.path.join(base_dir, "models", "eval_results.json")
    test_cases_path: str = os.path.join(base_dir, "tests", "test_cases.json")

DISCLAIMER_TEXT: str = (
    "AI DECISION-SUPPORT PROTOTYPE – NOT A CLINICAL OR MEDICAL DIAGNOSIS. "
    "Prototype weighting — not clinically validated. This module produces risk indicators for human review."
)

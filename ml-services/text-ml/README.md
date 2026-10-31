# SIH26093 - AI-Based Real-Time Stress & Trauma Assessment Module (Text ML)

> **IMPORTANT DISCLAIMER:**
> **AI DECISION-SUPPORT PROTOTYPE – NOT A CLINICAL OR MEDICAL DIAGNOSIS.**
> This module predicts 10 text-based risk and vulnerability indicators to assist human-in-the-loop assessment workflows. It does **not** diagnose PTSD, clinical depression, or psychiatric disorders.

---

## 1. Project Purpose
The **Text ML Module** processes victim/complainant narrative statements (in English, Indian languages, and Romanized/code-mixed Indic text) to identify **10 distinct risk and vulnerability indicators**. It calculates a normalized **Text Score (0–100)** and flags **Safety-Critical Incidents** for prioritized human review.

```
Victim Narrative Text
          ↓
  Text Preprocessing
          ↓
  Google MuRIL Encoder (google/muril-base-cased)
          ↓
  Fine-Tuned 10-Label Classification Head (Sigmoid outputs)
          ↓
  10 Text Risk Indicators (Probabilities 0.0 – 1.0)
          ↓
  Separate Text Scoring Engine (0 – 100) & Critical Safety Flags
          ↓
  FastAPI REST API + Web Dashboard
```

*(Note: In the complete SIH26093 system, Text Score + Voice Score will later combine to produce the overall Stress and Vulnerability Index (SVI). MuRIL does **NOT** directly predict SVI.)*

---

## 2. Architecture & Design Principles

### Why Google MuRIL (`google/muril-base-cased`)?
- **Multilingual & Indic Native:** MuRIL (Multilingual Representations for Indic Languages) is pre-trained by Google specifically on 17 Indian languages and transliterated Romanized Indic text.
- **Code-Mixed Resilience:** Robustly handles code-mixed narratives (e.g., Hinglish: *"Usne mujhe dhamki di ki wo mere ghar waalon ko maarega"*).

### Why Multi-Label Classification & Sigmoid (Not Softmax)?
- Victim statements frequently express multiple concurrent stress indicators simultaneously (e.g., physical violence + threat to family + immediate danger).
- **10 Independent Sigmoid Outputs:** Softmax forces labels to compete (summing to 1.0), which is incorrect for multi-indicator narratives. Sigmoid computes an independent probability $P(\text{label}_i \in [0, 1])$ per indicator using `BCEWithLogitsLoss`.

---

## 3. The 10 Output Indicators

| # | Indicator Label | Description |
|---|----------------|-------------|
| 1 | `violence` | Physical assault, beating, or bodily harm |
| 2 | `sexual_violence` | Sexual assault, molestation, or abuse |
| 3 | `threat_intimidation` | Verbal death threats, intimidation, or coercion |
| 4 | `immediate_danger` | Active current threat, perpetrator outside, hiding |
| 5 | `social_isolation` | Cut off from family/friends, forced isolation |
| 6 | `displacement` | Forced out of home, sleeping on street, homelessness |
| 7 | `family_harm` | Threats or physical violence directed at family/children |
| 8 | `self_harm_suicidal_ideation` | Expressions of wanting to end life or self-harm |
| 9 | `depression_related` | Feelings of severe hopelessness, helpless suffering |
| 10 | `legal_distress` | Case delays, court distress, lawyer/document trouble |

---

## 4. Text Score & Safety Flag Calculation

### Text Score (0 – 100)
Calculated via a **decoupled scoring engine** as a normalized weighted sum of indicator probabilities:
$$\text{Text Score} = 100 \times \frac{\sum_{i=1}^{10} P_i \cdot w_i}{\sum_{i=1}^{10} w_i}$$

*Disclaimer: Prototype scoring weights (`violence: 12.0`, `immediate_danger: 18.0`, etc.) require domain expert validation before clinical deployment.*

### Critical Safety Flags
Independent alert triggers for high-severity indicators (`self_harm_suicidal_ideation ≥ 0.40`, `immediate_danger ≥ 0.50`) ensure critical cases are routed to emergency human review regardless of the overall aggregate score.

---

## 5. Project Directory Structure

```
text-ml/
├── data/
│   ├── development/
│   │   ├── dev_dataset.csv
│   │   └── dev_dataset.jsonl
│   ├── train/          # Split training set
│   ├── validation/     # Split validation set
│   └── test/           # Split test set
├── models/
│   ├── muril_multilabel/ # Saved model weights, tokenizer, config
│   └── eval_results.json
├── src/
│   ├── config.py       # Model hyperparams, label definitions & weights
│   ├── dataset.py      # Dataset loader & Group-based splitting
│   ├── preprocessing.py# Transformer-safe text cleaner (preserves negation)
│   ├── model.py        # MuRIL PyTorch module with 10-label classifier head
│   ├── train.py        # BCEWithLogitsLoss training pipeline & pos_weight
│   ├── evaluate.py     # Micro/Macro F1, Hamming loss, threshold analysis
│   ├── scoring.py      # Decoupled Text Score engine & safety flags
│   ├── explainability.py# Non-hallucinating evidence span extractor
│   └── inference.py    # Unified prediction service with dev fallback
├── api/
│   └── main.py         # FastAPI REST service (/predict, /health)
├── frontend/
│   ├── index.html      # UI Dashboard
│   ├── styles.css      # Modern Glassmorphic styling
│   └── app.js          # Interactive dashboard controller
├── tests/
│   ├── test_cases.json # Test scenarios A through I
│   └── run_tests.py    # Contextual & negation test runner
├── requirements.txt
├── README.md
└── .gitignore
```

---

## 6. Setup & Execution Commands

### Step 1: Install Dependencies
```bash
pip install -r requirements.txt
```

### Step 2: Run Training Pipeline
```bash
python -m src.train
```
*Trains MuRIL on development dataset with group-based splitting and pos_weight calculation, saving model checkpoint to `models/muril_multilabel`.*

### Step 3: Run Contextual Test Suite (Cases A – I)
```bash
python -m tests.run_tests
```

### Step 4: Launch FastAPI REST Backend
```bash
python -m api.main
# Or: uvicorn api.main:app --reload --port 8000
```
*Swagger UI docs available at: `http://localhost:8000/docs`*

### Step 5: Launch Frontend Dashboard
Open `frontend/index.html` in any browser or serve via Python:
```bash
python -m http.server 8080 --directory frontend
```

---

## 7. Sample API Request & Response

### Request (`POST http://localhost:8000/predict`)
```json
{
  "text": "He threatened me yesterday and said he would hurt my family."
}
```

### Response
```json
{
  "text": "He threatened me yesterday and said he would hurt my family.",
  "indicators": {
    "violence": 0.05,
    "sexual_violence": 0.05,
    "threat_intimidation": 0.92,
    "immediate_danger": 0.05,
    "social_isolation": 0.05,
    "displacement": 0.05,
    "family_harm": 0.88,
    "self_harm_suicidal_ideation": 0.05,
    "depression_related": 0.05,
    "legal_distress": 0.05
  },
  "text_score": 25.8,
  "critical_flags": [
    {
      "indicator": "threat_intimidation",
      "probability": 0.92,
      "threshold": 0.65,
      "severity": "HIGH",
      "action_required": "Route narrative for immediate human-in-the-loop workflow (Threat Intimidation)"
    }
  ],
  "has_critical_flag": true,
  "evidence": {
    "threat_intimidation": "He threatened me yesterday and said he would hurt my family",
    "family_harm": "He threatened me yesterday and said he would hurt my family"
  },
  "model": {
    "name": "google/muril-base-cased",
    "task": "multi-label classification",
    "num_labels": 10,
    "is_checkpoint_loaded": true
  },
  "disclaimer": "AI DECISION-SUPPORT PROTOTYPE – NOT A CLINICAL OR MEDICAL DIAGNOSIS."
}
```

---

## 8. Replacing Development Data with Real Approved Datasets

To replace demonstration data with an ethically approved, anonymized real dataset:
1. Format real anonymized dataset into `data/train/real_dataset.csv` with columns:
   `case_id,text,violence,sexual_violence,threat_intimidation,immediate_danger,social_isolation,displacement,family_harm,self_harm_suicidal_ideation,depression_related,legal_distress`
2. Update `src/config.py` path to point `dev_data_path` to your real dataset CSV.
3. Re-run `python -m src.train` to fine-tune MuRIL on real data.

---

## 9. Switching Model Backbone to IndicBERT

To compare baseline performance with AI4Bharat's IndicBERT:
1. Open `src/config.py`.
2. Change `model_name_or_path` in `ModelConfig`:
   ```python
   model_name_or_path: str = "ai4bharat/indic-bert"
   ```
3. Re-run `python -m src.train`.

---

## 10. Ethical Considerations & Limitations
- **Privacy & Anonymization:** Raw victim narratives must never be logged to external analytics or unencrypted disk in production.
- **Human-in-the-Loop:** Automated emergency interventions or legal decisions must never be triggered solely by model outputs.
- **Biases in Transliteration:** Romanized Indic text spellings vary significantly across dialects; additional domain fine-tuning is required before clinical use.

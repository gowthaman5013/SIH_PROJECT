# VictimCare AI — AI-Assisted Decision-Support Prototype for First-Contact Victim/Complainant Assessment

> ⚠️ **PROTOTYPE DISCLAIMER**: This application is an **AI-assisted decision-support prototype**. It identifies distress, vulnerability, and incident indicators to assist human prioritization. It **does NOT** provide clinical diagnosis, medical evaluation, or autonomous police, legal, or emergency decisions. SVI weights, scoring rules, and thresholds are prototype configurations subject to domain expert review and are **not clinically validated**. Mandatory human review is enforced for all high-risk and critical safety situations.

---

## 1. System Architecture & Input Modes

The VictimCare AI decision-support system operates in two distinct evaluation modes:

### Mode A — Text-Only Mode
Used when victim text or officer–victim interaction transcripts are submitted without audio.
```text
Victim/Officer Narrative Text
        ↓
PII-Masking & Sanitization
        ↓
Sarvam Multilingual LLM Fact Extraction
        ↓
Deterministic Context Scoring Engine (0–100)
        ↓
Google MuRIL Pretrained Multilingual Transformer Backbone (10 Multi-Label Indicators)
        ↓
Text ML Risk Score (0–100)
        ↓
Text-Only SVI Calculator (70% Text ML + 30% Context Score)
        ↓
SVI Risk Categorization (LOW / MODERATE / HIGH / CRITICAL)
        ↓
Independent Safety Override Check (Does NOT modify mathematical SVI)
        ↓
Evidence-Based Support Prioritization Engine
```
*Note: Voice ML is set to `voice_available = false` and excluded from the SVI calculation in Text-Only mode.*

### Mode B — Multimodal Mode (Text + Audio)
Fuses Text ML Risk Score (40%), Voice ML Acoustic Risk Score (30%), and Structured Context Risk Score (30%).

---

## 2. Model Architecture & Status Identification

| Component | Technology Backbone | Status / Type | Explanation |
| :--- | :--- | :--- | :--- |
| **Text ML** | `google/muril-base-cased` | **Pretrained Multilingual Backbone** | Pretrained transformer backbone for multilingual text understanding across 10 multi-label indicator classifications. |
| **Voice ML** | Exotel Dual-Channel Stereo WAV + Librosa Acoustic Features + Wav2Vec2 | **Acoustic Feature Extraction Pipeline** | Victim-channel extraction analyzing pitch instability, shimmer, energy variation, and **Reduced Speaking Rate** (formerly "Trauma-Induced Freeze"). |
| **Structured Context** | Sarvam Multilingual LLM + Deterministic Engine | **LLM Fact Extractor + Rule Engine** | Sarvam LLM extracts structured JSON facts (`immediate_danger`, `physical_injury_reported`, `weapon_present`, etc.). The deterministic context scoring engine then calculates the 0–100 context score. |
| **SVI Calculation** | Linear Weighted Combination Engine | **Prototype Configuration** | Computes continuous SVI score (0–100) mathematically using backend single source of truth. |
| **Safety Override** | Monotonic Safety Layer | **Deterministic Guardrail** | Evaluates critical safety triggers (`immediate_danger = true`, `active_threat`, self-harm) independently without altering mathematical SVI score. |

---

## 3. Standard 10-Indicator Text ML Schema

All Text ML predictions utilize one unified schema across Python, Java DTOs, API responses, and Frontend UI:

1. `physical_violence` (Distinguishes actual physical assault e.g. "hit me", "punched me" from verbal threats)
2. `threat_intimidation` (Handles threats of harm e.g. "threatening to hurt me")
3. `weapon_life_threat`
4. `family_abuse`
5. `financial_deprivation`
6. `property_dispossession`
7. `stalking_cyber_harassment`
8. `child_dependent_exposure`
9. `repeat_offence`
10. `institutional_denial`

*Contextual "Hurt" Differentiation*: The phrase *"He is threatening to hurt me"* triggers `threat_intimidation` (verbal threat), NOT `physical_violence` or medical injury. Physical violence requires actual physical assault evidence ("he hit me", "injured my arm").

---

## 4. Structured Context Extraction & Deterministic Scoring Engine

The Sarvam LLM extracts structured facts:
```json
{
  "immediate_danger": true,
  "perpetrator_currently_present": true,
  "active_threat": true,
  "weapon_present": false,
  "physical_injury_reported": false,
  "medical_help_requested": false,
  "legal_help_requested": false,
  "protection_need": "HIGH",
  "help_urgency": "CRITICAL",
  "victim_currently_safe": false,
  "victim_alone": true,
  "witness_status": false,
  "retaliation_concern": false
}
```
The **Deterministic Context Scoring Engine** calculates `context_risk_score` (0–100) from these extracted facts using transparent, explainable scoring weights (`context_scoring.json`), ensuring reproducibility.

---

## 5. Mathematical SVI Calculation & Categorization

### Text-Only SVI Formula
$$\text{SVI} = (\text{Text ML Risk Score} \times 0.70) + (\text{Structured Context Risk Score} \times 0.30)$$

### Multimodal SVI Formula
$$\text{SVI} = (\text{Text ML Risk Score} \times 0.40) + (\text{Voice ML Risk Score} \times 0.30) + (\text{Structured Context Risk Score} \times 0.30)$$

### Prototype Risk Thresholds
- **0.00 – 24.99**: `LOW`
- **25.00 – 49.99**: `MODERATE`
- **50.00 – 74.99**: `HIGH`
- **75.00 – 100.00**: `CRITICAL`

*"Prototype risk categorization thresholds — not clinically validated."*

---

## 6. Separation of SVI Score from Safety Override

The system maintains a strict separation between mathematical risk scoring and safety override guardrails:

Example:
- Text ML: 23.6, Context Score: 35.0 $\rightarrow$ **SVI = 27.02** (Category: **MODERATE**)
- If `immediate_danger = true`, the **Safety Override Layer** fires separately:
  - `override_triggered = true`
  - `human_review_required = true`
  - `priority_alert = true`
  - `priority = "CRITICAL"`

The mathematical SVI remains **27.02** and SVI category remains **MODERATE**. The system outputs both Numerical Assessment and Safety Override Assessment without altering mathematical values.

---

## 7. Evidence-Based Support Prioritization Engine

Support recommendations are derived from explicit factual evidence rather than simple SVI score thresholds:

| Support Domain | Condition / Evidence Required | Priority |
| :--- | :--- | :--- |
| **`EMERGENCY_SUPPORT`** | `immediate_danger = true` OR active life-safety threat | `URGENT` |
| **`POLICE_INTERVENTION`** | Active threat, ongoing violence, weapon usage, or protection concern | `URGENT` / `HIGH` |
| **`MEDICAL_ASSISTANCE`** | Actual physical injury reported OR explicit medical help request | `URGENT` / `HIGH` |
| **`LEGAL_AID`** | Explicit legal help requested OR legal process assistance concern | `HIGH` / `NORMAL` |
| **`PSYCHOSOCIAL_COUNSELLING`** | Severe emotional distress, fear, anxiety, or victim request | `HIGH` / `NORMAL` |
| **`WITNESS_PROTECTION_CONSIDERATION`** | `witness_status = true` AND `retaliation_concern = true` | `URGENT` / `HIGH` |

Every recommendation returns a detailed `evidence` array citing the exact text/context facts.

---

## 8. Human-in-the-Loop & Prototype Limitations

1. **Mandatory Human Review**: High-risk cases and safety overrides require human administrator review prior to taking any case action.
2. **No Autonomous Dispatch**: The system displays recommendations as *"Human review required"* and *"Protection review recommended"*, never *"Police must be dispatched"*.
3. **Validation Notice**: Model performance must be evaluated using an ethically collected, anonymized, expert-annotated dataset prior to real-world deployment.

---

## 9. API Endpoint & Response Format

`POST /api/v1/assessment`

### Standard Response Payload
```json
{
  "mode": "TEXT_ONLY",
  "caseId": "NHAA-8492",
  "status": "AWAITING_ADMIN_REVIEW",

  "text_ml": {
    "risk_score": 23.6,
    "model": "google/muril-base-cased (Pretrained Multilingual Backbone)",
    "indicators": {
      "threat_intimidation": { "probability": 0.75, "value": true, "explanation": "Threatening language detected" }
    }
  },

  "voice_ml": {
    "available": false,
    "risk_score": null
  },

  "structured_context": {
    "facts": {
      "immediate_danger": true,
      "victim_alone": true
    },
    "risk_score": 35.0
  },

  "svi": {
    "formula": "(Text * 0.70) + (Context * 0.30)",
    "text_weight": 0.70,
    "voice_weight": 0.00,
    "context_weight": 0.30,
    "score": 27.02,
    "category": "MODERATE"
  },

  "safety": {
    "override_triggered": true,
    "priority": "CRITICAL",
    "human_review_required": true,
    "priority_alert": true,
    "reasons": ["Immediate danger detected in victim context."]
  },

  "support_prioritization": [
    {
      "support_type": "EMERGENCY_SUPPORT",
      "priority": "URGENT",
      "reason": "Immediate safety concern detected.",
      "evidence": ["Perpetrator is currently outside victim house."],
      "human_review_required": true
    }
  ],

  "validation": {
    "status": "PROTOTYPE_NOT_CLINICALLY_VALIDATED"
  }
}
```

---

## 10. Automated Test Suite Execution

Run the backend unit test suite covering all 10 required test scenarios:
```bash
mvn test
```
All **27 unit tests** pass with 100% success (`BUILD SUCCESS`), verifying mathematical SVI calculations, contextual "hurt" differentiation, safety override separation, evidence generation, and support mapping.
#   S I H _ P R O J E C T  
 
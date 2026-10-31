import os
import json
import numpy as np
import pandas as pd
from typing import Dict, Any, List
from sklearn.metrics import (
    precision_score,
    recall_score,
    f1_score,
    hamming_loss,
    roc_auc_score,
    average_precision_score
)

from src.config import INDICATOR_LABELS, PathConfig

def evaluate_multilabel_predictions(
    y_true: np.ndarray,
    y_prob: np.ndarray,
    threshold: float = 0.5,
    label_names: List[str] = INDICATOR_LABELS
) -> Dict[str, Any]:
    """
    Calculate comprehensive multi-label evaluation metrics.
    
    Metrics included:
    - Micro Precision, Recall, F1
    - Macro Precision, Recall, F1
    - Hamming Loss
    - Per-label Precision, Recall, F1, ROC-AUC, PR-AUC
    """
    y_pred = (y_prob >= threshold).astype(int)
    
    # Overall multi-label metrics
    micro_p = float(precision_score(y_true, y_pred, average="micro", zero_division=0))
    micro_r = float(recall_score(y_true, y_pred, average="micro", zero_division=0))
    micro_f1 = float(f1_score(y_true, y_pred, average="micro", zero_division=0))
    
    macro_p = float(precision_score(y_true, y_pred, average="macro", zero_division=0))
    macro_r = float(recall_score(y_true, y_pred, average="macro", zero_division=0))
    macro_f1 = float(f1_score(y_true, y_pred, average="macro", zero_division=0))
    
    h_loss = float(hamming_loss(y_true, y_pred))
    
    # Per-label detailed metrics
    per_label_metrics = {}
    for i, name in enumerate(label_names):
        p = float(precision_score(y_true[:, i], y_pred[:, i], zero_division=0))
        r = float(recall_score(y_true[:, i], y_pred[:, i], zero_division=0))
        f1 = float(f1_score(y_true[:, i], y_pred[:, i], zero_division=0))
        
        # ROC-AUC and PR-AUC where label contains both positive and negative samples
        try:
            auc = float(roc_auc_score(y_true[:, i], y_prob[:, i])) if len(np.unique(y_true[:, i])) > 1 else None
        except Exception:
            auc = None
            
        try:
            pr_auc = float(average_precision_score(y_true[:, i], y_prob[:, i])) if len(np.unique(y_true[:, i])) > 1 else None
        except Exception:
            pr_auc = None
            
        per_label_metrics[name] = {
            "precision": round(p, 4),
            "recall": round(r, 4),
            "f1": round(f1, 4),
            "roc_auc": round(auc, 4) if auc is not None else None,
            "pr_auc": round(pr_auc, 4) if pr_auc is not None else None,
            "positive_count": int(np.sum(y_true[:, i]))
        }
        
    return {
        "micro_precision": round(micro_p, 4),
        "micro_recall": round(micro_r, 4),
        "micro_f1": round(micro_f1, 4),
        "macro_precision": round(macro_p, 4),
        "macro_recall": round(macro_r, 4),
        "macro_f1": round(macro_f1, 4),
        "hamming_loss": round(h_loss, 4),
        "threshold_used": threshold,
        "per_label": per_label_metrics
    }

def run_threshold_analysis(
    y_true: np.ndarray,
    y_prob: np.ndarray,
    thresholds: List[float] = None,
    label_names: List[str] = INDICATOR_LABELS
) -> Dict[str, Any]:
    """
    Search across candidate probability thresholds (0.10 to 0.90) to find optimal F1 threshold per label.
    """
    if thresholds is None:
        thresholds = [round(t, 2) for t in np.arange(0.10, 0.95, 0.05)]
        
    optimal_thresholds = {}
    
    for i, name in enumerate(label_names):
        best_t = 0.5
        best_f1 = -1.0
        
        for t in thresholds:
            preds = (y_prob[:, i] >= t).astype(int)
            score = f1_score(y_true[:, i], preds, zero_division=0)
            if score > best_f1:
                best_f1 = score
                best_t = t
                
        optimal_thresholds[name] = {
            "optimal_threshold": float(best_t),
            "best_f1": round(float(best_f1), 4)
        }
        
    return optimal_thresholds

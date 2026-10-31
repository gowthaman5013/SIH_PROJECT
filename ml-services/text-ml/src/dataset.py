import os
import pandas as pd
import numpy as np
import torch
from torch.utils.data import Dataset
from typing import Dict, List, Tuple, Optional
from sklearn.model_selection import GroupShuffleSplit, train_test_split

from src.config import INDICATOR_LABELS, ModelConfig, PathConfig
from src.preprocessing import preprocess_text

class VictimTextDataset(Dataset):
    """
    PyTorch Dataset for victim text narratives tokenized for MuRIL multi-label classification.
    """
    def __init__(self, texts: List[str], labels: Optional[np.ndarray], tokenizer, max_length: int = 256):
        self.texts = [preprocess_text(t) for t in texts]
        self.labels = labels
        self.tokenizer = tokenizer
        self.max_length = max_length

    def __len__(self) -> int:
        return len(self.texts)

    def __getitem__(self, idx: int) -> Dict[str, torch.Tensor]:
        text = str(self.texts[idx])
        
        encoding = self.tokenizer(
            text,
            max_length=self.max_length,
            padding="max_length",
            truncation=True,
            return_tensors="pt"
        )
        
        item = {
            "input_ids": encoding["input_ids"].squeeze(0),
            "attention_mask": encoding["attention_mask"].squeeze(0)
        }
        
        if self.labels is not None:
            item["labels"] = torch.tensor(self.labels[idx], dtype=torch.float32)
            
        return item

def load_and_validate_dataset(csv_or_jsonl_path: str) -> pd.DataFrame:
    """Load dataset file (CSV or JSONL) and validate presence of required columns."""
    if not os.path.exists(csv_or_jsonl_path):
        raise FileNotFoundError(f"Dataset path not found: {csv_or_jsonl_path}")
        
    if csv_or_jsonl_path.endswith(".csv"):
        df = pd.read_csv(csv_or_jsonl_path)
    elif csv_or_jsonl_path.endswith(".jsonl"):
        df = pd.read_json(csv_or_jsonl_path, lines=True)
    else:
        raise ValueError("Unsupported dataset format. Use .csv or .jsonl")
        
    required_columns = ["text"] + INDICATOR_LABELS
    for col in required_columns:
        if col not in df.columns:
            raise KeyError(f"Required column '{col}' missing from dataset.")
            
    if "case_id" not in df.columns:
        # Fallback: create artificial unique case_ids if none present
        df["case_id"] = [f"CASE_{i:04d}" for i in range(len(df))]
        
    return df

def split_dataset_group_based(
    df: pd.DataFrame,
    test_size: float = 0.2,
    val_size: float = 0.15,
    random_seed: int = 42
) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """
    Split dataset into Train, Validation, and Test sets while preserving group structure (case_id/person_id)
    to prevent data leakage across splits.
    """
    groups = df["case_id"].values
    
    # First split into train_val and test sets using group splitting if multiple samples share groups
    unique_groups = df["case_id"].nunique()
    
    if unique_groups >= len(df) * 0.8 and unique_groups > 5:
        # Standard group shuffle split
        gss = GroupShuffleSplit(n_splits=1, test_size=test_size, random_state=random_seed)
        train_val_idx, test_idx = next(gss.split(df, groups=groups))
        
        train_val_df = df.iloc[train_val_idx].reset_index(drop=True)
        test_df = df.iloc[test_idx].reset_index(drop=True)
        
        # Split train_val into train and val
        val_relative_size = val_size / (1.0 - test_size)
        gss_val = GroupShuffleSplit(n_splits=1, test_size=val_relative_size, random_state=random_seed)
        train_idx, val_idx = next(gss_val.split(train_val_df, groups=train_val_df["case_id"].values))
        
        train_df = train_val_df.iloc[train_idx].reset_index(drop=True)
        val_df = train_val_df.iloc[val_idx].reset_index(drop=True)
    else:
        # Fallback for small dev datasets where unique groups equal dataset size
        train_val_df, test_df = train_test_split(df, test_size=test_size, random_state=random_seed)
        val_relative_size = val_size / (1.0 - test_size)
        train_df, val_df = train_test_split(train_val_df, test_size=val_relative_size, random_state=random_seed)
        
    return train_df.reset_index(drop=True), val_df.reset_index(drop=True), test_df.reset_index(drop=True)

def compute_pos_weights(train_labels: np.ndarray) -> torch.Tensor:
    """
    Compute positive class weights (pos_weight) for BCEWithLogitsLoss strictly from the TRAINING set
    to handle class imbalance without data leakage.
    Formula: pos_weight = (total_samples - pos_samples) / (pos_samples + 1e-5)
    """
    pos_counts = np.sum(train_labels, axis=0)
    total_samples = train_labels.shape[0]
    neg_counts = total_samples - pos_counts
    
    # Avoid zero division
    pos_weights = (neg_counts + 1e-5) / (pos_counts + 1e-5)
    # Clip extreme weights for training stability
    pos_weights = np.clip(pos_weights, 0.5, 10.0)
    
    return torch.tensor(pos_weights, dtype=torch.float32)

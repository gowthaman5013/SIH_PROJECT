import os
import json
import torch
import numpy as np
import pandas as pd
from torch.utils.data import DataLoader
from transformers import AutoTokenizer, get_linear_schedule_with_warmup
from typing import Dict, Any

from src.config import ModelConfig, PathConfig, INDICATOR_LABELS
from src.dataset import (
    load_and_validate_dataset,
    split_dataset_group_based,
    VictimTextDataset,
    compute_pos_weights
)
from src.model import MuRILMultiLabelClassifier, get_loss_function
from src.evaluate import evaluate_multilabel_predictions

def set_seed(seed: int = 42):
    """Set random seeds for reproducibility."""
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    np.random.seed(seed)

def train_model(
    model_config: ModelConfig = ModelConfig(),
    path_config: PathConfig = PathConfig(),
    data_df: pd.DataFrame = None
) -> Dict[str, Any]:
    """
    Complete training pipeline for MuRIL 10-label multi-label classifier.
    """
    set_seed(model_config.random_seed)
    os.makedirs(path_config.model_save_dir, exist_ok=True)
    os.makedirs(path_config.checkpoint_dir, exist_ok=True)

    # 1. Load dataset if not provided
    if data_df is None:
        data_df = load_and_validate_dataset(path_config.dev_data_path)
        
    print(f"[TRAIN] Total samples loaded: {len(data_df)}")

    # 2. Data splitting (Group-based to prevent leakage across splits)
    train_df, val_df, test_df = split_dataset_group_based(
        data_df,
        test_size=0.2,
        val_size=0.15,
        random_seed=model_config.random_seed
    )
    print(f"[TRAIN] Train split: {len(train_df)} | Val split: {len(val_df)} | Test split: {len(test_df)}")

    # Save split files for transparency
    os.makedirs(os.path.join(path_config.data_dir, "train"), exist_ok=True)
    os.makedirs(os.path.join(path_config.data_dir, "validation"), exist_ok=True)
    os.makedirs(os.path.join(path_config.data_dir, "test"), exist_ok=True)
    train_df.to_csv(path_config.train_data_path, index=False)
    val_df.to_csv(path_config.val_data_path, index=False)
    test_df.to_csv(path_config.test_data_path, index=False)

    # 3. Calculate pos_weight strictly from training split
    train_labels = train_df[INDICATOR_LABELS].values.astype(np.float32)
    val_labels = val_df[INDICATOR_LABELS].values.astype(np.float32)
    test_labels = test_df[INDICATOR_LABELS].values.astype(np.float32)

    if model_config.use_pos_weight:
        pos_weight = compute_pos_weights(train_labels)
        print(f"[TRAIN] Computed pos_weight from training set: {pos_weight.numpy().round(3)}")
    else:
        pos_weight = None

    # 4. Tokenizer & DataLoaders
    print(f"[TRAIN] Loading tokenizer for: {model_config.model_name_or_path}")
    tokenizer = AutoTokenizer.from_pretrained(model_config.model_name_or_path)

    train_dataset = VictimTextDataset(train_df["text"].tolist(), train_labels, tokenizer, model_config.max_length)
    val_dataset = VictimTextDataset(val_df["text"].tolist(), val_labels, tokenizer, model_config.max_length)

    train_loader = DataLoader(train_dataset, batch_size=model_config.batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=model_config.batch_size, shuffle=False)

    # 5. Model Initialization & Device
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[TRAIN] Training device: {device}")

    model = MuRILMultiLabelClassifier(
        model_name_or_path=model_config.model_name_or_path,
        num_labels=model_config.num_labels,
        dropout_rate=model_config.dropout_rate
    ).to(device)

    loss_fn = get_loss_function(pos_weight.to(device) if pos_weight is not None else None)

    optimizer = torch.optim.AdamW(model.parameters(), lr=model_config.learning_rate, weight_decay=model_config.weight_decay)
    total_steps = len(train_loader) * model_config.num_epochs
    scheduler = get_linear_schedule_with_warmup(optimizer, num_warmup_steps=int(total_steps * 0.1), num_training_steps=total_steps)

    # 6. Training Loop
    best_val_f1 = -1.0
    best_model_path = os.path.join(path_config.model_save_dir, "pytorch_model.bin")

    for epoch in range(1, model_config.num_epochs + 1):
        model.train()
        train_loss = 0.0

        for batch in train_loader:
            optimizer.zero_grad()
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            labels = batch["labels"].to(device)

            outputs = model(input_ids, attention_mask)
            logits = outputs["logits"]

            loss = loss_fn(logits, labels)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            optimizer.step()
            scheduler.step()

            train_loss += loss.item()

        avg_train_loss = train_loss / len(train_loader)

        # Validation Loop
        model.eval()
        val_loss = 0.0
        val_probs_list, val_targets_list = [], []

        with torch.no_grad():
            for batch in val_loader:
                input_ids = batch["input_ids"].to(device)
                attention_mask = batch["attention_mask"].to(device)
                labels = batch["labels"].to(device)

                outputs = model(input_ids, attention_mask)
                logits = outputs["logits"]
                probs = outputs["probabilities"]

                loss = loss_fn(logits, labels)
                val_loss += loss.item()

                val_probs_list.append(probs.cpu().numpy())
                val_targets_list.append(labels.cpu().numpy())

        avg_val_loss = val_loss / len(val_loader)
        val_probs = np.vstack(val_probs_list)
        val_targets = np.vstack(val_targets_list)

        val_metrics = evaluate_multilabel_predictions(val_targets, val_probs, threshold=model_config.default_threshold)
        macro_f1 = val_metrics["macro_f1"]

        print(f"Epoch {epoch}/{model_config.num_epochs} | Train Loss: {avg_train_loss:.4f} | Val Loss: {avg_val_loss:.4f} | Val Macro F1: {macro_f1:.4f}")

        # Checkpoint saving based on best Macro F1
        if macro_f1 > best_val_f1:
            best_val_f1 = macro_f1
            print(f" -> Saving new best model checkpoint (Val Macro F1: {best_val_f1:.4f})")
            torch.save(model.state_dict(), best_model_path)
            tokenizer.save_pretrained(path_config.model_save_dir)

            # Save configuration and label mapping
            meta_info = {
                "model_name": model_config.model_name_or_path,
                "num_labels": model_config.num_labels,
                "indicator_labels": INDICATOR_LABELS,
                "max_length": model_config.max_length,
                "best_val_macro_f1": float(best_val_f1),
                "threshold": model_config.default_threshold
            }
            with open(os.path.join(path_config.model_save_dir, "config.json"), "w") as f:
                json.dump(meta_info, f, indent=2)

    print(f"[TRAIN] Training completed. Best Validation Macro F1: {best_val_f1:.4f}")
    return {
        "best_val_macro_f1": best_val_f1,
        "model_dir": path_config.model_save_dir
    }

if __name__ == "__main__":
    train_model()

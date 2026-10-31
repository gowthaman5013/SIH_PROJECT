import torch
import torch.nn as nn
from transformers import AutoModel, AutoConfig
from typing import Dict, Optional

class MuRILMultiLabelClassifier(nn.Module):
    """
    Multi-Label Classifier architecture based on Google MuRIL (google/muril-base-cased).
    
    Architecture Flow:
        Input Text 
            ↓
        MuRIL Tokenizer 
            ↓
        MuRIL Transformer Encoder
            ↓
        Contextual Representation ([CLS] / Pooler Output)
            ↓
        Dropout (0.2)
            ↓
        Linear Classification Head (Hidden Dim -> 10 Logits)
            ↓
        Sigmoid Activation (per logit)
            ↓
        10 Risk/Vulnerability Probabilities (0.0 to 1.0)
    """
    def __init__(self, model_name_or_path: str = "google/muril-base-cased", num_labels: int = 10, dropout_rate: float = 0.2, pretrained: bool = True):
        super(MuRILMultiLabelClassifier, self).__init__()
        self.model_name_or_path = model_name_or_path
        self.num_labels = num_labels
        
        # Load transformer configuration and backbone encoder
        self.config = AutoConfig.from_pretrained(model_name_or_path)
        if pretrained:
            self.transformer = AutoModel.from_pretrained(model_name_or_path, config=self.config)
        else:
            self.transformer = AutoModel.from_config(self.config)
        
        hidden_size = self.config.hidden_size
        self.dropout = nn.Dropout(dropout_rate)
        self.classifier = nn.Linear(hidden_size, num_labels)
        
    def forward(
        self,
        input_ids: torch.Tensor,
        attention_mask: torch.Tensor,
        token_type_ids: Optional[torch.Tensor] = None
    ) -> Dict[str, torch.Tensor]:
        """
        Forward pass through MuRIL encoder and 10-label classification head.
        Returns dictionary containing raw 'logits' and 'probabilities' (sigmoid).
        """
        # Pass through transformer encoder
        kwargs = {"input_ids": input_ids, "attention_mask": attention_mask}
        if token_type_ids is not None:
            kwargs["token_type_ids"] = token_type_ids
            
        outputs = self.transformer(**kwargs)
        
        # Extract contextual representation: use pooler_output if available, else mean of last_hidden_state
        if hasattr(outputs, "pooler_output") and outputs.pooler_output is not None:
            cls_rep = outputs.pooler_output
        else:
            # Mean pooling over non-padded tokens
            last_hidden = outputs.last_hidden_state
            mask_expanded = attention_mask.unsqueeze(-1).expand(last_hidden.size()).float()
            cls_rep = torch.sum(last_hidden * mask_expanded, 1) / torch.clamp(mask_expanded.sum(1), min=1e-9)
            
        pooled_output = self.dropout(cls_rep)
        logits = self.classifier(pooled_output)  # Raw unnormalized logits (shape: [batch_size, 10])
        probabilities = torch.sigmoid(logits)     # Independent sigmoid outputs (range: [0.0, 1.0])
        
        return {
            "logits": logits,
            "probabilities": probabilities
        }

def get_loss_function(pos_weight: Optional[torch.Tensor] = None) -> nn.BCEWithLogitsLoss:
    """
    Returns BCEWithLogitsLoss for multi-label classification.
    Supports pos_weight to address class imbalance.
    """
    if pos_weight is not None:
        return nn.BCEWithLogitsLoss(pos_weight=pos_weight)
    return nn.BCEWithLogitsLoss()

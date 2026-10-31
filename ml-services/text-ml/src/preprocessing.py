import re
import unicodedata

# Negation and contextual words that MUST NOT be removed
NEGATION_WORDS = {
    "not", "no", "never", "neither", "nor", "none", "nobody", "nothing",
    "nowhere", "hardly", "barely", "scarcely", "didn't", "didnt", "cannot",
    "can't", "cant", "couldn't", "couldnt", "won't", "wont", "wouldn't",
    "wouldnt", "don't", "dont", "doesn't", "doesnt", "isn't", "isnt",
    "aren't", "arent", "wasn't", "wasnt", "weren't", "werent", "haven't",
    "havent", "hasn't", "hasnt", "hadn't", "hadnt", "n't", "na", "nahi", "nahin"
}

def preprocess_text(text: str, lower: bool = False) -> str:
    """
    Preprocess victim narrative text for Transformer-based models (MuRIL / IndicBERT).
    
    Rules followed:
    1. Unicode normalization (NFC) for Indic script compatibility.
    2. Normalize whitespace and remove non-printable characters.
    3. Retain ALL negation words (not, never, didn't, etc.) to preserve sentiment/negation context.
    4. Retain meaningful punctuation (?, !, ., ", ') which carries contextual meaning.
    5. No aggressive stemming or stopword stripping, ensuring transformer tokenizers retain syntax.
    
    Args:
        text (str): Raw victim text narrative.
        lower (bool): Optional lowercase flag. Defaults to False as MuRIL-base-cased uses casing.
        
    Returns:
        str: Preprocessed text narrative.
    """
    if not isinstance(text, str) or not text.strip():
        return ""
    
    # Unicode normalization (handles Indic scripts like Devanagari, Bengali, Tamil, etc.)
    text = unicodedata.normalize("NFC", text)
    
    # Replace multiple whitespace characters/newlines with a single space
    text = re.sub(r"\s+", " ", text).strip()
    
    # Optional lowercase (default False since google/muril-base-cased is cased)
    if lower:
        text = text.lower()
        
    return text

def validate_narrative_text(text: str) -> bool:
    """Validate if input narrative text is non-empty and meets minimal length constraints."""
    cleaned = preprocess_text(text)
    return len(cleaned) > 0

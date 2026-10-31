import os
import sys
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional

# Add src to python path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.inference import TextMLInferencePipeline
from src.config import DISCLAIMER_TEXT

app = FastAPI(
    title="SIH26093 - Text ML Risk Assessment API",
    description="Real-time multi-label risk and vulnerability indicator assessment from victim narratives using Google MuRIL.",
    version="1.0.0"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Inference Pipeline singleton
pipeline = TextMLInferencePipeline()

class PredictionRequest(BaseModel):
    text: str = Field(..., example="He threatened me yesterday and said he would hurt my family.")

class PredictionResponse(BaseModel):
    text: str
    indicators: Dict[str, float]
    text_score: float
    critical_flags: List[Dict[str, Any]]
    has_critical_flag: bool
    evidence: Dict[str, Optional[str]]
    model: Dict[str, Any]
    disclaimer: str

# Mount frontend static directory
frontend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "frontend"))
if os.path.exists(frontend_dir):
    app.mount("/static", StaticFiles(directory=frontend_dir), name="static")

@app.get("/")
def read_root():
    index_path = os.path.join(frontend_dir, "index.html")
    if os.path.exists(index_path):
        return FileResponse(index_path)
    return {
        "service": "SIH26093 Text ML Module API",
        "status": "online",
        "model": "google/muril-base-cased",
        "docs": "/docs"
    }

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "pytorch_model_loaded": pipeline.is_pytorch_model_loaded,
        "device": str(pipeline.device)
    }

@app.post("/predict", response_model=PredictionResponse)
def predict_narrative_risk(payload: PredictionRequest):
    if not payload.text or not payload.text.strip():
        raise HTTPException(status_code=400, detail="Text narrative cannot be empty.")
        
    try:
        result = pipeline.predict(payload.text)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Inference error: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)

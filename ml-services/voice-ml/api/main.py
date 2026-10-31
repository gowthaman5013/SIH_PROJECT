import os
import sys
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Dict, Any, Optional

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.voice_ml import VoiceMLAnalyzer

app = FastAPI(
    title="NHAA - Voice ML Risk Assessment API",
    description="Real-time 7 Voice Evidence indicator analysis and Voice Risk Score generation.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

analyzer = VoiceMLAnalyzer()

class AudioRequest(BaseModel):
    audio_path: Optional[str] = "demo.wav"

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "Voice ML Module"}

@app.post("/predict")
def predict_voice_risk(payload: AudioRequest):
    try:
        res = analyzer.analyze_audio(payload.audio_path or "demo.wav")
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)

import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8000';

const PRESETS = {
  "CASE_A": { label: "⚡ Positive Threat", text: "He threatened me yesterday and said he would break my legs." },
  "CASE_B": { label: "🛡️ Negated Threat", text: "He did not threaten me, we just had a verbal disagreement." },
  "CASE_C": { label: "👨‍👩‍👧 Family Harm", text: "He threatened to hurt my children if I talked to anyone." },
  "CASE_D": { label: "🚨 Immediate Danger", text: "He is outside my house right now pounding on the door and threatening to kill me." },
  "CASE_E": { label: "🆘 Self Harm", text: "I feel like ending my life because I cannot bear this suffering anymore." },
  "CASE_F": { label: "⚖️ Legal Distress", text: "I have been going to court for eight months and feel completely helpless because the case is not moving forward." },
  "CASE_G": { label: "💥 Multi-Indicator", text: "He assaulted me, threatened my brother, and now we are afraid to go back to our home." },
  "CASE_H": { label: "❓ Reported Question", text: "The police officer asked me whether he threatened me or harmed me." },
  "CASE_I": { label: "🚫 Negation + Family", text: "He never threatened my family or tried to hurt them." },
  "CASE_HINGLISH": { label: "🇮🇳 Code-Mixed Indic", text: "Usne mujhe dhamki di ki agar maine police ko bataya toh wo mere ghar waalon ko maarega." }
};

export default function App() {
  const [narrative, setNarrative] = useState('');
  const [loading, setLoading] = useState(false);
  const [apiOnline, setApiOnline] = useState(false);
  const [result, setResult] = useState(null);

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/health`);
      if (res.ok) {
        setApiOnline(true);
      } else {
        setApiOnline(false);
      }
    } catch {
      setApiOnline(false);
    }
  };

  const handleAnalyze = async () => {
    if (!narrative.trim()) {
      alert("Please enter a victim statement narrative before analyzing.");
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE_URL}/predict`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: narrative.trim() })
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.detail || "Prediction failed");
      }

      const data = await res.json();
      setResult(data);
    } catch (err) {
      alert(`API Error: ${err.message}\nMake sure FastAPI backend is running on http://localhost:8000`);
    } finally {
      setLoading(false);
    }
  };

  const handleClear = () => {
    setNarrative('');
    setResult(null);
  };

  // SVG Gauge circumference = 2 * PI * 50 = 314
  const textScore = result ? result.text_score : 0.0;
  const strokeDashoffset = 314 - (314 * (textScore / 100));

  let gaugeColor = "#38bdf8";
  if (textScore >= 70) gaugeColor = "#f43f5e";
  else if (textScore >= 40) gaugeColor = "#f59e0b";

  return (
    <div class="app-wrapper">
      {/* Top Navbar */}
      <header class="navbar">
        <div class="nav-brand">
          <div class="brand-icon">SIH</div>
          <div>
            <h1 class="brand-title">SIH26093 <span class="badge-tag">Text ML Module</span></h1>
            <p class="brand-sub">AI-Based Real-Time Stress & Trauma Assessment System</p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span class="port-badge">Vite Dev Server: Port 5173</span>
          <span class={`port-badge ${apiOnline ? '' : 'offline'}`} style={{ background: apiOnline ? 'rgba(16, 185, 129, 0.15)' : 'rgba(244, 63, 94, 0.15)', color: apiOnline ? '#10b981' : '#f43f5e' }}>
            API (8000): {apiOnline ? 'Online ✓' : 'Offline ✗'}
          </span>
        </div>
      </header>

      {/* Disclaimer Banner */}
      <div class="disclaimer-banner">
        <span class="disclaimer-badge">AI DECISION SUPPORT PROTOTYPE</span>
        <span>
          Outputs 10 multi-label text risk indicators for human-in-the-loop review. This system does <strong>NOT</strong> provide a clinical diagnosis for PTSD, depression, or mental health disorders.
        </span>
      </div>

      <main class="dashboard-grid">
        {/* Left Input Panel */}
        <section class="grid-panel">
          <div class="panel-header">
            <h2>Victim Narrative Input</h2>
            <span class="char-counter">{narrative.length} characters</span>
          </div>

          {/* Presets */}
          <div class="preset-section">
            <span class="preset-label">Quick Scenario Presets:</span>
            <div class="preset-pills">
              {Object.keys(PRESETS).map(key => (
                <button
                  key={key}
                  class="preset-pill"
                  onClick={() => setNarrative(PRESETS[key].text)}
                >
                  {PRESETS[key].label}
                </button>
              ))}
            </div>
          </div>

          {/* Text Area */}
          <div class="textarea-wrapper">
            <textarea
              value={narrative}
              onChange={(e) => setNarrative(e.target.value)}
              placeholder="Enter victim/complainant narrative statement in English, Hindi, or Romanized Indic text (e.g., 'He threatened me yesterday and said he would hurt my family')..."
            />
          </div>

          {/* Action Row */}
          <div class="action-bar">
            <button class="analyze-btn" onClick={handleAnalyze} disabled={loading}>
              {loading ? <div class="spinner" /> : <span>⚡ Analyze Narrative</span>}
            </button>
            <button class="clear-btn" onClick={handleClear}>Clear</button>
          </div>

          {/* Model Info */}
          <div class="model-meta-card">
            <div class="meta-row">
              <span class="meta-key">Pretrained Encoder:</span>
              <span class="meta-val">Google MuRIL (google/muril-base-cased)</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">Classifier Output:</span>
              <span class="meta-val">10 Independent Sigmoid Probabilities</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">Loss Function:</span>
              <span class="meta-val">BCEWithLogitsLoss</span>
            </div>
            <div class="meta-row">
              <span class="meta-key">Scoring Decoupling:</span>
              <span class="meta-val">Text Score (0–100) computed separately</span>
            </div>
          </div>
        </section>

        {/* Right Results Panel */}
        <section class="grid-panel">
          {/* Critical Flags Alert */}
          {result && result.has_critical_flag && (
            <div class="critical-box">
              <div class="critical-title">
                <span>🚨</span> CRITICAL SAFETY FLAGS TRIGGERED
              </div>
              {result.critical_flags.map((flag, i) => (
                <div key={i} class="critical-item">
                  <strong>[{flag.severity}] {flag.indicator.replace(/_/g, ' ').toUpperCase()}</strong>
                  &nbsp;(Prob: {(flag.probability * 100).toFixed(1)}% | Threshold: {(flag.threshold * 100).toFixed(0)}%)
                  <div style={{ fontSize: '11px', marginTop: '2px', color: '#fca5a5' }}>
                    {flag.action_required}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Text Score Highlight Card */}
          <div class="score-highlight-card">
            <div class="score-info">
              <h3>Normalized Text Score</h3>
              <p class="score-desc">Normalized weighted sum of the 10 indicator probabilities (0.0 to 100.0)</p>
              <span class="weights-tag">Prototype weights – Requires domain expert validation</span>
            </div>

            <div class="gauge-box">
              <svg class="gauge-svg" viewBox="0 0 120 120">
                <circle class="gauge-bg" cx="60" cy="60" r="50" />
                <circle
                  class="gauge-ring"
                  cx="60"
                  cy="60"
                  r="50"
                  style={{
                    strokeDashoffset,
                    stroke: gaugeColor
                  }}
                />
              </svg>
              <div class="gauge-center">
                <span class="score-display-num" style={{ color: gaugeColor }}>
                  {textScore.toFixed(1)}
                </span>
                <span class="score-scale">/ 100</span>
              </div>
            </div>
          </div>

          {/* 10 Indicators Cards */}
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '16px', marginBottom: '14px' }}>
              10 Text Risk Indicators
            </h3>
            <div class="indicators-grid">
              {result ? (
                Object.keys(result.indicators).map(label => {
                  const prob = result.indicators[label];
                  const pct = (prob * 100).toFixed(1);

                  let badge = "LOW";
                  if (prob >= 0.70) badge = "CRITICAL";
                  else if (prob >= 0.50) badge = "HIGH";
                  else if (prob >= 0.30) badge = "MODERATE";

                  return (
                    <div key={label} class="indicator-card">
                      <div class="ind-head">
                        <span class="ind-name">{label.replace(/_/g, ' ')}</span>
                        <span class={`ind-badge ${badge}`}>{badge}</span>
                      </div>
                      <div class="ind-pct">{pct}%</div>
                      <div class="ind-bar-track">
                        <div class={`ind-bar-fill ${badge}`} style={{ width: `${pct}%` }} />
                      </div>
                    </div>
                  );
                })
              ) : (
                <div class="empty-state" style={{ gridColumn: '1 / -1' }}>
                  Enter narrative text and click "Analyze Narrative" to view risk score and indicator probabilities.
                </div>
              )}
            </div>
          </div>

          {/* Extracted Evidence Spans */}
          <div>
            <h3 style={{ fontFamily: 'var(--font-heading)', fontSize: '16px', marginBottom: '12px' }}>
              Extracted Verbatim Text Evidence Spans
            </h3>
            <div class="evidence-list">
              {result && result.evidence && Object.values(result.evidence).some(v => v !== null) ? (
                Object.keys(result.evidence).map(label => {
                  const sentence = result.evidence[label];
                  if (!sentence) return null;
                  return (
                    <div key={label} class="evidence-item">
                      <div class="evidence-tag">{label.replace(/_/g, ' ')}</div>
                      <div>"{sentence}"</div>
                    </div>
                  );
                })
              ) : (
                <div class="empty-state">
                  No active verbatim evidence spans mapped (evidence is null when no exact text span is matched).
                </div>
              )}
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
